package com.exchange.tests;

import com.exchange.platform.entity.ChatMessage;
import com.exchange.platform.entity.ChatRoom;
import com.exchange.platform.entity.User;
import com.exchange.platform.repository.ChatMessageRepository;
import com.exchange.platform.repository.ChatRoomRepository;
import com.exchange.platform.repository.UserRepository;
import com.exchange.platform.service.ChatService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = com.exchange.platform.ExchangeWebAppApplication.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChatRoomStressTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public JavaMailSender mockJavaMailSender() {
            JavaMailSender mock = mock(JavaMailSender.class);
            MimeMessage mimeMessage = new MimeMessage((Session) null);
            when(mock.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mock).send(any(MimeMessage.class));
            return mock;
        }
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatService chatService;

    static class LoadTestResult {
        int concurrentUsers;
        int successCount;
        int failureCount;
        long minResponseTime;
        long maxResponseTime;
        double avgResponseTime;
        long p50;
        long p95;
        long p99;
        double errorRate;
        double throughput;
        long totalTimeMs;
    }

    private final List<LoadTestResult> testResults = new ArrayList<>();
    private final String REPORT_DIR = "target/stress-test-reports/";

    @BeforeEach
    public void setUp() throws Exception {
        File reportDir = new File(REPORT_DIR);
        if (!reportDir.exists()) {
            reportDir.mkdirs();
        }
        testResults.clear();
    }

    @AfterEach
    public void tearDown() {
        try {
            chatMessageRepository.deleteAll();
            chatRoomRepository.deleteAll();
            userRepository.deleteAll();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    @Order(1)
    @DisplayName("PST-02: 漸進式聊天室訊息壓力測試")
    public void testProgressiveChatMessageStress() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PST-02: 漸進式聊天室訊息壓力測試 - 尋找系統極限");
        System.out.println("=".repeat(80) + "\n");

        int startConcurrency = 2000;
        int incrementStep = 2000;
        int maxConcurrency = 20000;
        double maxErrorRate = 10.0;
        long maxP95ResponseTime = 5000;

        System.out.println("測試配置：");
        System.out.println("  起始併發數: " + startConcurrency);
        System.out.println("  每輪增加: " + incrementStep);
        System.out.println("  最大併發數: " + maxConcurrency);
        System.out.println("  停止條件: 錯誤率>" + maxErrorRate + "% 或 P95>" + maxP95ResponseTime + "ms\n");

        boolean systemLimitReached = false;
        int roundNumber = 1;

        for (int concurrency = startConcurrency; concurrency <= maxConcurrency; concurrency += incrementStep) {
            System.out.println("─".repeat(80));
            System.out.println("第 " + roundNumber + " 輪測試 - 併發數: " + concurrency);
            System.out.println("─".repeat(80));

            LoadTestResult result = performChatLoadTest(concurrency);
            testResults.add(result);

            System.out.println("\n測試結果：");
            System.out.println(String.format("  併發數=%d, 成功=%d, 失敗=%d, 錯誤率=%.2f%%",
                result.concurrentUsers, result.successCount, result.failureCount, result.errorRate));
            System.out.println(String.format("  響應時間(ms): Min=%d, P50=%d, P95=%d, P99=%d, Max=%d, Avg=%.2f",
                result.minResponseTime, result.p50, result.p95, result.p99, result.maxResponseTime, result.avgResponseTime));
            System.out.println(String.format("  吞吐量: %.2f msg/s\n", result.throughput));

            if (result.errorRate > maxErrorRate) {
                System.out.println("  ⚠ 系統達到極限：錯誤率 " + String.format("%.2f%%", result.errorRate) + " 超過閾值");
                systemLimitReached = true;
                break;
            }

            if (result.p95 > maxP95ResponseTime) {
                System.out.println("  ⚠ 系統達到極限：P95響應時間 " + result.p95 + "ms 超過閾值");
                systemLimitReached = true;
                break;
            }

            if (concurrency < maxConcurrency) {
                System.out.println("  等待3秒，讓系統恢復...\n");
                Thread.sleep(3000);
            }

            roundNumber++;
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("測試完成總結");
        System.out.println("=".repeat(80));
        System.out.println("總測試輪數: " + testResults.size());
        System.out.println("系統極限: " + (systemLimitReached ? "已達到" : "未達到"));
        
        if (!testResults.isEmpty()) {
            LoadTestResult bestResult = testResults.stream()
                .filter(r -> r.errorRate < 5.0)
                .max(Comparator.comparingInt(r -> r.concurrentUsers))
                .orElse(testResults.get(0));
            
            System.out.println("\n最佳性能點（錯誤率 < 5%）：");
            System.out.println(String.format("  併發數=%d, 成功=%d, P95=%dms, 錯誤率=%.2f%%",
                bestResult.concurrentUsers, bestResult.successCount, bestResult.p95, bestResult.errorRate));
            
            LoadTestResult lastResult = testResults.get(testResults.size() - 1);
            System.out.println("\n系統能承受的最大併發訊息數: " + lastResult.concurrentUsers);
        }
        System.out.println("=".repeat(80) + "\n");

        generatePerformanceCharts();
        exportResultsToCSV();

        System.out.println("📊 測試報告已生成：");
        System.out.println("   響應時間趨勢圖: " + new File(REPORT_DIR + "chat-response-time-chart.png").getAbsolutePath());
        System.out.println("   錯誤率趨勢圖: " + new File(REPORT_DIR + "chat-error-rate-chart.png").getAbsolutePath());
        System.out.println("   吞吐量趨勢圖: " + new File(REPORT_DIR + "chat-throughput-chart.png").getAbsolutePath());
        System.out.println("   原始數據CSV: " + new File(REPORT_DIR + "chat-test-results.csv").getAbsolutePath());
        System.out.println();
    }

    private LoadTestResult performChatLoadTest(int concurrency) throws Exception {
        // 建立測試用戶和聊天室
        List<User> testUsers = new ArrayList<>();
        List<ChatRoom> testChatRooms = new ArrayList<>();
        
        for (int i = 0; i < concurrency; i++) {
            // 創建發送者用戶
            User sender = User.builder()
                    .email("chat_sender_" + System.currentTimeMillis() + "_" + i + "@test.com")
                    .passwordHash("password123")
                    .displayName("Chat Sender " + i)
                    .verified(true)
                    .roles("USER")
                    .build();
            sender = userRepository.save(sender);
            testUsers.add(sender);
            
            // 創建接收者用戶
            User receiver = User.builder()
                    .email("chat_receiver_" + System.currentTimeMillis() + "_" + i + "@test.com")
                    .passwordHash("password123")
                    .displayName("Chat Receiver " + i)
                    .verified(true)
                    .roles("USER")
                    .build();
            receiver = userRepository.save(receiver);
            testUsers.add(receiver);
            
            // 創建聊天室
            ChatRoom chatRoom = new ChatRoom();
            chatRoom.setProposalId(1000000L + i);  // 使用唯一的假 proposal ID
            chatRoom.setUserAId(sender.getId());
            chatRoom.setUserBId(receiver.getId());
            chatRoom.setStatus(ChatRoom.ChatRoomStatus.ACTIVE);
            chatRoom = chatRoomRepository.save(chatRoom);
            testChatRooms.add(chatRoom);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(Math.min(concurrency, 50));
        CountDownLatch latch = new CountDownLatch(testChatRooms.size());
        ConcurrentLinkedQueue<Long> responseTimes = new ConcurrentLinkedQueue<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < testChatRooms.size(); i++) {
            final ChatRoom chatRoom = testChatRooms.get(i);
            final User sender = testUsers.get(i * 2);
            
            executorService.submit(() -> {
                try {
                    long requestStart = System.nanoTime();
                    
                    // 發送文字訊息
                    ChatMessage message = chatService.sendTextMessage(
                        chatRoom.getId(), 
                        sender.getId(), 
                        "Test message: " + System.currentTimeMillis()
                    );

                    long requestEnd = System.nanoTime();
                    long responseTimeMs = (requestEnd - requestStart) / 1_000_000;

                    responseTimes.add(responseTimeMs);

                    if (message != null && message.getId() != null) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(120, TimeUnit.SECONDS);
        executorService.shutdown();
        
        long endTime = System.currentTimeMillis();
        long totalTimeMs = endTime - startTime;

        LoadTestResult result = new LoadTestResult();
        result.concurrentUsers = concurrency;
        result.successCount = successCount.get();
        result.failureCount = failureCount.get();
        result.totalTimeMs = totalTimeMs;

        List<Long> sortedTimes = responseTimes.stream().sorted().collect(Collectors.toList());

        if (!sortedTimes.isEmpty()) {
            result.minResponseTime = sortedTimes.get(0);
            result.maxResponseTime = sortedTimes.get(sortedTimes.size() - 1);
            result.avgResponseTime = sortedTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
            result.p50 = calculatePercentile(sortedTimes, 50);
            result.p95 = calculatePercentile(sortedTimes, 95);
            result.p99 = calculatePercentile(sortedTimes, 99);
        }

        int totalRequests = result.successCount + result.failureCount;
        result.errorRate = totalRequests > 0 ? (result.failureCount * 100.0) / totalRequests : 0.0;
        result.throughput = totalRequests / (totalTimeMs / 1000.0);

        // 清理測試數據
        chatMessageRepository.deleteAll(chatMessageRepository.findAll().stream()
            .filter(msg -> testChatRooms.stream().anyMatch(room -> room.getId().equals(msg.getChatRoomId())))
            .collect(Collectors.toList()));
        chatRoomRepository.deleteAll(testChatRooms);
        userRepository.deleteAll(testUsers);

        return result;
    }

    private long calculatePercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    private void generatePerformanceCharts() throws Exception {
        // 設置中文字體
        Font chineseFont = new Font("Microsoft JhengHei", Font.PLAIN, 14);
        Font chineseTitleFont = new Font("Microsoft JhengHei", Font.BOLD, 18);
        
        // 響應時間趨勢圖
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries p50Series = new XYSeries("P50");
        XYSeries p95Series = new XYSeries("P95");
        XYSeries p99Series = new XYSeries("P99");

        for (LoadTestResult result : testResults) {
            p50Series.add(result.concurrentUsers, result.p50);
            p95Series.add(result.concurrentUsers, result.p95);
            p99Series.add(result.concurrentUsers, result.p99);
        }

        dataset.addSeries(p50Series);
        dataset.addSeries(p95Series);
        dataset.addSeries(p99Series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "聊天室壓力測試 - 響應時間趨勢",
                "併發訊息數",
                "響應時間 (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        // 設置標題字體
        chart.getTitle().setFont(chineseTitleFont);
        
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // 設置軸標籤字體
        plot.getDomainAxis().setLabelFont(chineseFont);
        plot.getDomainAxis().setTickLabelFont(chineseFont);
        plot.getRangeAxis().setLabelFont(chineseFont);
        plot.getRangeAxis().setTickLabelFont(chineseFont);
        
        // 設置圖例字體
        chart.getLegend().setItemFont(chineseFont);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, new Color(0, 150, 0));
        renderer.setSeriesPaint(1, new Color(255, 165, 0));
        renderer.setSeriesPaint(2, new Color(255, 0, 0));
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));
        renderer.setSeriesStroke(2, new BasicStroke(2.0f));
        plot.setRenderer(renderer);

        ChartUtils.saveChartAsPNG(new File(REPORT_DIR + "chat-response-time-chart.png"), chart, 1200, 600);

        // 錯誤率趨勢圖
        XYSeries errorRateSeries = new XYSeries("錯誤率");
        for (LoadTestResult result : testResults) {
            errorRateSeries.add(result.concurrentUsers, result.errorRate);
        }
        XYSeriesCollection errorDataset = new XYSeriesCollection(errorRateSeries);
        JFreeChart errorChart = ChartFactory.createXYLineChart(
                "聊天室壓力測試 - 錯誤率趨勢", "併發訊息數", "錯誤率 (%)", errorDataset,
                PlotOrientation.VERTICAL, true, true, false);
        
        // 設置錯誤率圖表的中文字體
        errorChart.getTitle().setFont(chineseTitleFont);
        XYPlot errorPlot = errorChart.getXYPlot();
        errorPlot.getDomainAxis().setLabelFont(chineseFont);
        errorPlot.getDomainAxis().setTickLabelFont(chineseFont);
        errorPlot.getRangeAxis().setLabelFont(chineseFont);
        errorPlot.getRangeAxis().setTickLabelFont(chineseFont);
        errorChart.getLegend().setItemFont(chineseFont);
        
        ChartUtils.saveChartAsPNG(new File(REPORT_DIR + "chat-error-rate-chart.png"), errorChart, 1200, 600);

        // 吞吐量趨勢圖
        XYSeries throughputSeries = new XYSeries("吞吐量");
        for (LoadTestResult result : testResults) {
            throughputSeries.add(result.concurrentUsers, result.throughput);
        }
        XYSeriesCollection throughputDataset = new XYSeriesCollection(throughputSeries);
        JFreeChart throughputChart = ChartFactory.createXYLineChart(
                "聊天室壓力測試 - 吞吐量趨勢", "併發訊息數", "吞吐量 (msg/s)", throughputDataset,
                PlotOrientation.VERTICAL, true, true, false);
        
        // 設置吞吐量圖表的中文字體
        throughputChart.getTitle().setFont(chineseTitleFont);
        XYPlot throughputPlot = throughputChart.getXYPlot();
        throughputPlot.getDomainAxis().setLabelFont(chineseFont);
        throughputPlot.getDomainAxis().setTickLabelFont(chineseFont);
        throughputPlot.getRangeAxis().setLabelFont(chineseFont);
        throughputPlot.getRangeAxis().setTickLabelFont(chineseFont);
        throughputChart.getLegend().setItemFont(chineseFont);
        
        ChartUtils.saveChartAsPNG(new File(REPORT_DIR + "chat-throughput-chart.png"), throughputChart, 1200, 600);
    }

    private void exportResultsToCSV() throws Exception {
        try (FileWriter writer = new FileWriter(new File(REPORT_DIR + "chat-test-results.csv"))) {
            writer.write("併發訊息數,成功數,失敗數,錯誤率(%),Min(ms),P50(ms),P95(ms),P99(ms),Max(ms),Avg(ms),吞吐量(msg/s)\n");
            for (LoadTestResult result : testResults) {
                writer.write(String.format("%d,%d,%d,%.2f,%d,%d,%d,%d,%d,%.2f,%.2f\n",
                        result.concurrentUsers, result.successCount, result.failureCount, result.errorRate,
                        result.minResponseTime, result.p50, result.p95, result.p99,
                        result.maxResponseTime, result.avgResponseTime, result.throughput));
            }
        }
    }
}
