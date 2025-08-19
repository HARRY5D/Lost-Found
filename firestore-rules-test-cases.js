// Test script for Firebase Firestore rules
// Run this in Firebase Console Rules Playground or with Firebase Emulator

// Test Data Setup
const testUsers = {
  alice: { uid: "alice123", email: "alice@example.com" },
  bob: { uid: "bob456", email: "bob@example.com" },
  anonymous: null
};

const testLostItem = {
  id: "lost001",
  name: "iPhone 13",
  description: "Black iPhone with cracked screen",
  category: "Electronics",
  location: "Library",
  reportedBy: "alice123",
  reportedByName: "Alice Johnson",
  reportedDate: new Date(),
  dateLost: new Date()
};

const testFoundItem = {
  id: "found001",
  name: "Car Keys",
  description: "Toyota keys with blue keychain",
  category: "Keys",
  location: "Parking Lot",
  reportedBy: "bob456",
  reportedByName: "Bob Smith",
  reportedDate: new Date(),
  dateFound: new Date(),
  keptAt: "Security Office",
  claimed: false,
  claimedBy: "",
  claimedByName: ""
};

// Test Cases for Rules Playground:

// 1. TEST: Alice can read all lost items (cross-user visibility)
// Resource: /databases/(default)/documents/lostItems/lost001
// Auth: alice123
// Operation: read
// Expected: ALLOW

// 2. TEST: Bob can read Alice's lost item (cross-user visibility)
// Resource: /databases/(default)/documents/lostItems/lost001
// Auth: bob456
// Operation: read
// Expected: ALLOW

// 3. TEST: Alice can create her own lost item
// Resource: /databases/(default)/documents/lostItems/new_item
// Auth: alice123
// Operation: create
// Data: { reportedBy: "alice123", name: "Wallet", ... }
// Expected: ALLOW

// 4. TEST: Bob cannot delete Alice's lost item (security)
// Resource: /databases/(default)/documents/lostItems/lost001
// Auth: bob456
// Operation: delete
// Expected: DENY

// 5. TEST: Bob can claim Alice's found item
// Resource: /databases/(default)/documents/foundItems/found001
// Auth: bob456
// Operation: update
// Data: { claimed: true, claimedBy: "bob456", claimedByName: "Bob Smith" }
// Expected: ALLOW

// 6. TEST: Anonymous user cannot read items (authentication required)
// Resource: /databases/(default)/documents/lostItems/lost001
// Auth: null
// Operation: read
// Expected: DENY
