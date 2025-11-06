import re
import time
import os
from datetime import datetime
import requests
from bs4 import BeautifulSoup

class CodeNotFound(Exception):
    pass
class VerifyError(Exception):
    pass

class ECTracker():
    def __init__(self):
        self.api = 'https://eservice.7-11.com.tw/E-Tracking'
        self.session = requests.Session()

    def get_resource(self):
        """取得網頁 cookies、headers 等資源，並下載最新驗證碼圖片"""
        # 步驟一：取得查詢頁面
        headers_get = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        }
        with self.session.get(f'{self.api}/search.aspx', headers=headers_get) as response:
            response.encoding = 'utf-8'  # 若執行有亂碼可改成 'big5'
            if response.status_code != 200:
                print('取得 search.aspx 失敗:', response.status_code)
                response.raise_for_status()
            cookies = response.cookies
            body = BeautifulSoup(response.text, 'html.parser')

        __VIEWSTATE = body.find('input', {'id':'__VIEWSTATE'}).get('value', None)
        __VIEWSTATEGENERATOR = body.find('input', {'id':'__VIEWSTATEGENERATOR'}).get('value', None)
        headers_tmp_cookie = []
        for k, v in cookies.get_dict().items():
            headers_tmp_cookie.append(f'{k}={v}')
        headers_img = {
            'Cookie': ';'.join(headers_tmp_cookie),
            'User-Agent': headers_get['User-Agent'],
            'Accept': 'image/webp,image/apng,image/*,*/*;q=0.8'
        }
        ts = str(int(time.time()*1000))
        code_url = f'{self.api}/ValidateImage.aspx?ts={ts}'
        print('驗證碼圖片網址:', code_url)
        img_path = os.path.join(os.getcwd(), 'codeImg.jpg')
        print('開始下載驗證碼圖片...')
        with self.session.get(code_url, headers=headers_img) as response:
            print('圖片下載 status:', response.status_code)
            if response.status_code != 200:
                print('取得驗證碼圖片失敗')
                response.raise_for_status()
            print('圖片檔案大小:', len(response.content))
            with open(img_path, 'wb') as file_io:
                file_io.write(response.content)
            print('已存到:', img_path)
        return {
            'headers': headers_img,
            '__VIEWSTATE': __VIEWSTATE,
            '__VIEWSTATEGENERATOR': __VIEWSTATEGENERATOR
        }

    def tracker(self, txtProductNum, code, resource):
        """單號 + 驗證碼即查詢"""
        if not code:
            raise CodeNotFound('請先輸入驗證碼')
        payload = {
            '__LASTFOCUS': '',
            '__EVENTTARGET': '',
            '__EVENTARGUMENT': '',
            '__VIEWSTATE': resource['__VIEWSTATE'],
            '__VIEWSTATEGENERATOR': resource['__VIEWSTATEGENERATOR'],
            'txtProductNum': txtProductNum,
            'tbChkCode': code,
            'aaa': '',
            'txtIMGName': '',
            'txtPage': '1'
        }
        with requests.post('https://eservice.7-11.com.tw/E-Tracking/search.aspx', headers=resource['headers'], data=payload, allow_redirects=False) as response:
            response.encoding = 'utf-8'  # 若有亂碼可改 'big5'
            if response.status_code != 200:
                print('查詢失敗:', response.status_code)
                response.raise_for_status()
            body = BeautifulSoup(response.text, 'html.parser')
            if (body.find('input', {'id': 'txtPage'})['value']) == '2':
                info_children = body.find('div', {'class': 'info'}).find_all('div', recursive=False)
                shipping = body.find('div', {'class': 'shipping'})
                pickup_info = info_children[0]
                # 取貨門市
                store_name = pickup_info.find('span', {'id': 'store_name'}).text
                # 取貨門市地址
                store_address = pickup_info.find('p', {'id': 'store_address'}).text
                # 取貨截止日
                pickup_deadline = pickup_info.find('span', {'id': 'deadline'}).text
                # 付款資訊
                payment_type = info_children[1].find('h4', {'id': 'servicetype'}).text
                # 貨態資訊
                status = []
                for element in shipping.find_all('li'):
                    status_date = re.findall(r"\d{4}/\d{2}/\d{2} \d{2}:\d{2}", element.text)[0]
                    status.append(status_date + ' ' + (element.text).replace(status_date, ''))
                status.reverse()
                tracker = {
                    '取貨門市': store_name,
                    '取貨門市地址': store_address,
                    '取貨截止日': pickup_deadline,
                    '付款資訊': payment_type,
                    '貨態資訊': status
                }
                return tracker
            raise VerifyError('查詢失敗，請確認驗證碼或交貨便單號是否正確')

if __name__ == '__main__':
    ECTRACKER = ECTracker()
    # 步驟一：取得驗證碼圖片
    resource = ECTRACKER.get_resource()
    print('已取得驗證碼圖片，請在 codeImg.jpg 查看並輸入驗證碼')
    code = input('請輸入驗證碼: ')
    result = ECTRACKER.tracker('J99170695484', code, resource)
    print(result)