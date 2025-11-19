MACHINE ClassExchange

SETS
  USER; LISTING; PROPOSAL; SWAP; SHIPMENT; MESSAGE; REVIEW; DISPUTE; MARKETPLACE

CONSTANTS
  MAX_LOGIN_ATTEMPTS, CONDITION_LEVELS, DELIVERY_METHODS, LISTING_STATUS, PROPOSAL_STATUS, SWAP_STATUS, SHIPMENT_STATUS, REVIEW_SCORES, ROLES

VARIABLES
  users, listings, proposals, swaps, shipments, messages, reviews, disputes, favorites, blacklists, marketplaces,
  user_status, user_roles, user_login_attempts, user_verified, user_risk, user_banned_until,
  listing_owner, listing_status, listing_locked_by, listing_condition, listing_tags,
  proposal_proposer, proposal_listing, proposal_status, proposal_expires,
  swap_parties, swap_status, swap_completed_at, swap_received_confirmed,
  shipment_swap, shipment_sender, shipment_method, shipment_tracking, shipment_status, shipment_events,
  message_proposal, message_swap, message_from, message_content, message_created_at,
  review_swap, review_reviewer, review_reviewed, review_scores, review_comment,
  dispute_swap, dispute_claimant, dispute_reason, dispute_evidence, dispute_status, dispute_resolution,
  favorite_user, favorite_listing, blacklist_user, blacklist_reason, marketplace_listings

INVARIANT
  /* Users */
  users \subseteq USER & user_status : users --> {"ACTIVE", "BANNED", "RISK"} & user_roles : users --> POW(ROLES) & user_verified : users --> BOOL & user_login_attempts : users --> NAT & user_risk : users --> NAT & user_banned_until : users --> NAT
  /* Listings */
  & listings \subseteq LISTING & listing_owner : listings --> users & listing_status : listings --> LISTING_STATUS & listing_locked_by : listings --> (PROPOSAL \cup {NULL}) & listing_condition : listings --> CONDITION_LEVELS & listing_tags : listings --> POW(STRING)
  /* Proposals */
  & proposals \subseteq PROPOSAL & proposal_proposer : proposals --> users & proposal_listing : proposals --> POW(listings) & proposal_status : proposals --> PROPOSAL_STATUS & proposal_expires : proposals --> NAT
  /* Swaps */
  & swaps \subseteq SWAP & swap_parties : swaps --> POW(users) & swap_status : swaps --> SWAP_STATUS & swap_completed_at : swaps --> NAT & swap_received_confirmed : swaps --> POW(users)
  /* Shipments */
  & shipments \subseteq SHIPMENT & shipment_swap : shipments --> swaps & shipment_sender : shipments --> users & shipment_method : shipments --> DELIVERY_METHODS & shipment_tracking : shipments --> STRING & shipment_status : shipments --> SHIPMENT_STATUS & shipment_events : shipments --> seq(STRING)
  /* Messages */
  & messages \subseteq MESSAGE & message_proposal : messages --> (PROPOSAL \cup {NULL}) & message_swap : messages --> (SWAP \cup {NULL}) & message_from : messages --> users & message_content : messages --> STRING & message_created_at : messages --> NAT
  /* Reviews */
  & reviews \subseteq REVIEW & review_swap : reviews --> swaps & review_reviewer : reviews --> users & review_reviewed : reviews --> users & review_scores : reviews --> seq(REVIEW_SCORES) & review_comment : reviews --> STRING
  /* Disputes */
  & disputes \subseteq DISPUTE & dispute_swap : disputes --> swaps & dispute_claimant : disputes --> users & dispute_reason : disputes --> STRING & dispute_evidence : disputes --> seq(STRING) & dispute_status : disputes --> {"OPEN", "RESOLVED"} & dispute_resolution : disputes --> STRING
  /* Favorites & Blacklists */
  & favorites \subseteq users * listings & blacklists \subseteq users * STRING
  /* Marketplaces */
  & marketplaces \subseteq MARKETPLACE & marketplace_listings : marketplaces --> POW(listings)

INITIALISATION
  users, listings, proposals, swaps, shipments, messages, reviews, disputes, favorites, blacklists, marketplaces := {},{},{},{},{},{},{},{},{},{},{}
  || user_status, user_roles, user_verified, user_login_attempts, user_risk, user_banned_until := {},{},{},{},{},{}
  || listing_owner, listing_status, listing_locked_by, listing_condition, listing_tags := {},{},{},{},{}
  || proposal_proposer, proposal_listing, proposal_status, proposal_expires := {},{},{},{}
  || swap_parties, swap_status, swap_completed_at, swap_received_confirmed := {},{},{},{}
  || shipment_swap, shipment_sender, shipment_method, shipment_tracking, shipment_status, shipment_events := {},{},{},{},{},{}
  || message_proposal, message_swap, message_from, message_content, message_created_at := {},{},{},{},{}
  || review_swap, review_reviewer, review_reviewed, review_scores, review_comment := {},{},{},{},{}
  || dispute_swap, dispute_claimant, dispute_reason, dispute_evidence, dispute_status, dispute_resolution := {},{},{},{},{},{}
  || favorite_user, favorite_listing, blacklist_user, blacklist_reason, marketplace_listings := {},{},{},{},{}

OPERATIONS

  /* UC-01: Authentication (Register / Login) */
  Register(u, email, password, display_name) =
    PRE u \notin users & valid_email(email) & strong_password(password) & email_unused(email)
    THEN
      users := users \/ {u} || user_status(u) := "ACTIVE" || user_roles(u) := {"ROLE_USER"} || user_verified(u) := FALSE || user_login_attempts(u) := 0
    END;

  VerifyEmail(u) =
    PRE u \in users & user_verified(u) = FALSE
    THEN user_verified(u) := TRUE END;

  Login(u, password) =
    PRE u \in users & user_status(u) = "ACTIVE" & check_password(u, password) & user_login_attempts(u) < MAX_LOGIN_ATTEMPTS
    THEN user_login_attempts(u) := 0 END;

  ForgotPassword(u, email) =
    PRE u \in users & user_email(u) = email
    THEN send_reset_email(u) END;

  ResetPassword(u, new_password) =
    PRE u \in users & strong_password(new_password)
    THEN set_password(u, new_password) END;

  /* UC-02: Create Listing */
  CreateListing(l, u, data) =
    PRE u \in users & user_status(u) = "ACTIVE" & l \notin listings
    THEN listings := listings \/ {l} || listing_owner(l) := u || listing_status(l) := "PENDING_REVIEW" || listing_condition(l) := data.condition || listing_tags(l) := data.tags END;

  ApproveListing(l, admin) =
    PRE l \in listings & listing_status(l) = "PENDING_REVIEW" & admin \in users & "ROLE_ADMIN" \in user_roles(admin)
    THEN listing_status(l) := "ACTIVE" END;

  /* UC-03: Search and Filter Listings */
  SearchListings(query, filters) =
    PRE TRUE
    THEN skip END;

  /* UC-04: Create Proposal */
  CreateProposal(p, proposer, target_listing, offered_listings, expires) =
    PRE proposer \in users & target_listing \in listings & listing_status(target_listing) = "ACTIVE" & listing_locked_by(target_listing) = NULL & (\forall l.(l \in offered_listings => listing_status(l) = "ACTIVE" & listing_locked_by(l) = NULL))
    THEN proposals := proposals \/ {p} || proposal_proposer(p) := proposer || proposal_listing(p) := offered_listings \/ {target_listing} || proposal_status(p) := "PENDING" || proposal_expires(p) := expires || (\forall l.(l \in offered_listings => listing_locked_by(l) := p)) END;

  RespondProposal(p, owner, response) =
    PRE p \in proposals & owner \in users & proposal_status(p) = "PENDING"
    THEN proposal_status(p) := response END;

  /* UC-05: Negotiation (In-app Chat) */
  SendMessage(m, p, from, content) =
    PRE p \in proposals & from \in users
    THEN messages := messages \/ {m} || message_proposal(m) := p || message_from(m) := from || message_content(m) := content END;

  /* UC-06: Exchange Confirmation and Shipping */
  CreateSwap(s, p, parties) =
    PRE p \in proposals & proposal_status(p) = "ACCEPTED_BY_B" & s \notin swaps
    THEN swaps := swaps \/ {s} || swap_parties(s) := parties || swap_status(s) := "INIT" END;

  CreateShipment(sh, s, sender, method, tracking) =
    PRE s \in swaps & sender \in swap_parties(s)
    THEN shipments := shipments \/ {sh} || shipment_swap(sh) := s || shipment_sender(sh) := sender || shipment_method(sh) := method || shipment_tracking(sh) := tracking END;

  /* UC-07: Shipment Tracking */
  UpdateShipmentStatus(sh, status, event) =
    PRE sh \in shipments
    THEN shipment_status(sh) := status || shipment_events(sh) := shipment_events(sh) ^ <event> END;

  /* UC-08: Delivery Confirmation and Completion */
  ConfirmReceived(s, u) =
    PRE s \in swaps & u \in swap_parties(s)
    THEN swap_received_confirmed(s) := swap_received_confirmed(s) \/ {u} END;

  CompleteSwap(s) =
    PRE s \in swaps & swap_received_confirmed(s) = swap_parties(s)
    THEN swap_status(s) := "COMPLETED" || swap_completed_at(s) := current_time END;

  /* UC-09: Review and Reputation */
  SubmitReview(r, s, reviewer, reviewed, scores, comment) =
    PRE s \in swaps & reviewer \in swap_parties(s) & reviewed \in swap_parties(s) & r \notin reviews
    THEN reviews := reviews \/ {r} || review_swap(r) := s || review_reviewer(r) := reviewer || review_reviewed(r) := reviewed || review_scores(r) := scores || review_comment(r) := comment END;

  /* UC-10: Dispute and Arbitration */
  CreateDispute(d, s, claimant, reason, evidence) =
    PRE s \in swaps & claimant \in swap_parties(s) & d \notin disputes
    THEN disputes := disputes \/ {d} || dispute_swap(d) := s || dispute_claimant(d) := claimant || dispute_reason(d) := reason || dispute_evidence(d) := evidence || dispute_status(d) := "OPEN" END;

  ResolveDispute(d, admin, resolution) =
    PRE d \in disputes & dispute_status(d) = "OPEN" & admin \in users & "ROLE_ADMIN" \in user_roles(admin)
    THEN dispute_status(d) := "RESOLVED" || dispute_resolution(d) := resolution END;

  /* UC-11: Favorites and Follow */
  AddFavorite(u, l) =
    PRE u \in users & l \in listings
    THEN favorites := favorites \/ {(u, l)} END;

  RemoveFavorite(u, l) =
    PRE (u, l) \in favorites
    THEN favorites := favorites \ {(u, l)} END;

  /* UC-12: Blacklist and Risk Alerts */
  AddBlacklist(u, reason) =  
    PRE u \in users
    THEN blacklists := blacklists \/ {(u, reason)} END;

  RemoveBlacklist(u) =
    PRE (u, reason) \in blacklists
    THEN blacklists := blacklists \ {(u, reason)} END;  

  /* UC-13: Condition Grading (reference, not an operation) */
  /* UC-14: Automatic Matching */
  AutoMatch(user, match_job) =
    PRE user \in users
    THEN skip END;

  /* UC-15: Themed Marketplace */
  CreateMarketplace(m, admin, title, start, end, allowed_categories) =
    PRE m \notin marketplaces & admin \in users & "ROLE_ADMIN" \in user_roles(admin)
    THEN marketplaces := marketplaces \/ {m} || marketplace_listings(m) := {} END;

  AddListingToMarketplace(m, l) =
    PRE m \in marketplaces & l \in listings
    THEN marketplace_listings(m) := marketplace_listings(m) \/ {l} END;

END
