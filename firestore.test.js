const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_segma_123";
const BOB_UID = "bob_segma_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function buildUserProfilePayload(uid, now = pastDate(10000)) {
  return {
    userId: uid,
    displayName: "Zhilan Nur Rayfan",
    roleTitle: "Digital Strategist",
    plan: "PRO",
    activeHandle: "@eitssampah",
    secondaryHandle: "@gedagezhi",
    instagramConnected: true,
    instagramHandle: "@eitssampah",
    tiktokConnected: false,
    tiktokHandle: "",
    createdAt: now,
    updatedAt: now,
  };
}

function buildCampaignPayload(uid, now = pastDate(10000)) {
  return {
    userId: uid,
    name: "Bring Your Own Box!",
    brandName: "Eits Sampah",
    handle: "@eitssampah",
    objective: "Increase student eco-awareness",
    description: "Campus sustainability Reels and TikTok series",
    targetAudience: "University students 18-22",
    audienceTags: ["Students 18-22", "Campus Hub", "Eco-Friendly"],
    platforms: ["Instagram", "TikTok"],
    duration: "14 Days",
    budget: "Organic",
    status: "ONGOING",
    demoData: true,
    targetReach: 15000,
    targetEr: 5.5,
    reach: 18450,
    views: 24200,
    likes: 980,
    comments: 142,
    shares: 96,
    saves: 55,
    engagementRate: 6.9,
    dataSourceLabel: "Demo Data • Updated 2h ago",
    aiOverview: "Peer-to-peer satirical eco-campaign.",
    aiCommunicationStrategy: "Avoid lecturing; use humor.",
    aiContentPillars: ["Campus POV", "Eco Hacks"],
    aiUploadStrategy: "Post 19:00-21:00 WIB",
    aiContentCalendar: "Day 1: Hook Reel\nDay 3: Poll Story",
    aiCaptions: "No shame in bringing your own lunchbox.",
    aiHashtags: ["#TrashIsAnIssueToo", "#ClearYourTrace"],
    aiAudioRecommendation: "Backburner - Niki",
    aiNextActionRecommendation: "Launch Part 2 within 3 days.",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated users are denied across all SEGMA.AI collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("campaigns").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("personas").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("evaluations").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("competitors").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("bookmarks").get());
});

test("UserProfile: owner create, partial update, and cross-user/shadow rejections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(buildUserProfilePayload(ALICE_UID))
  );

  // Action: edit profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Zhilan R.",
      updatedAt: pastDate(1000),
    })
  );

  // Action: connect TikTok
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      tiktokConnected: true,
      tiktokHandle: "@eitssampah.tt",
      updatedAt: pastDate(500),
    })
  );

  // Reject plan escalation (shadow update)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      plan: "AGENCY",
      updatedAt: pastDate(200),
    })
  );

  // Reject cross-user access
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: pastDate(100),
    })
  );
});

test("Campaigns: owner create, status update, strategy update, and validation guards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("campaigns");

  await assertSucceeds(col.doc("camp_1").set(buildCampaignPayload(ALICE_UID)));
  await assertSucceeds(col.get());

  // Status change
  await assertSucceeds(
    col.doc("camp_1").update({
      status: "COMPLETED",
      updatedAt: pastDate(1000),
    })
  );

  // Reject @DocumentId `id` field injection
  const withId = { ...buildCampaignPayload(ALICE_UID), id: "camp_2" };
  await assertFails(col.doc("camp_2").set(withId));

  // Reject invalid status enum
  await assertFails(
    col.doc("camp_1").update({
      status: "ARCHIVED",
      updatedAt: pastDate(500),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    col.doc("camp_1").update({
      status: "DRAFT",
      createdAt: pastDate(200),
      updatedAt: pastDate(200),
    })
  );

  // Reject future timestamp
  const futurePayload = buildCampaignPayload(ALICE_UID, new Date(Date.now() + 120000));
  await assertFails(col.doc("camp_future").set(futurePayload));

  // Reject cross-user list/read/write
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("campaigns").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("campaigns").doc("camp_1").get()
  );
});

test("Personas, Evaluations, Competitors, Bookmarks: owner CRUD & isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(8000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("personas").doc("p_1").set({
      userId: ALICE_UID,
      name: "Campus Trendsetters",
      ageRange: "18-22",
      location: "Jakarta",
      interests: ["Eco-Friendly", "Campus Life"],
      painPoints: "Dislikes preachy eco ads",
      preferredFormats: ["Reels", "TikTok"],
      toneOfVoice: "Witty peer-to-peer",
      feasibilityScore: 88,
      summary: "High affinity for relatable satirical campus clips.",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("evaluations").doc("e_1").set({
      userId: ALICE_UID,
      campaignId: "camp_1",
      campaignName: "Bring Your Own Box!",
      executionSummary: "Strong organic share growth.",
      objectiveAchievement: "Exceeded reach target by 23%.",
      targetVsActual: "Target 5.5% ER vs Actual 6.9% ER.",
      engagementAnalysis: "Saves and shares peaked 19:00-21:00.",
      contentEffectiveness: "POV Reels outperformed carousels.",
      strengths: "Authentic tone.",
      weaknesses: "Need stronger CTA in slide 1.",
      influencingFactors: "Trending audio fit.",
      futureRecommendations: "Launch Part 2 series.",
      overallScore: 91,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("competitors").doc("c_1").set({
      userId: ALICE_UID,
      brandName: "GreenCampus ID",
      username: "@greencampus.id",
      platform: "Instagram",
      dataMode: "AI Strategic Analysis (Manual/Demo Context)",
      contentThemes: "Zero-waste tips",
      publishingFrequency: "3 posts/week",
      dominantFormats: "Static Infographics",
      observedMetricsSummary: "Estimated ~2.1% ER (user-observed)",
      communicationStyle: "Formal educational",
      strengths: "Consistent branding",
      opportunities: "Missing short-form humor",
      differentiationStrategy: "Own relatable comedic Reels",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("b_1").set({
      userId: ALICE_UID,
      trendId: "trend_audio_top3",
      title: "Top 3 Viral Audio Tracks",
      category: "Music/Audio",
      summary: "Fast 2-second beat-drop hooks drive retention.",
      aiRecommendation: "Use Backburner lyric hook for campus satire.",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user isolation checks
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("personas").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("evaluations").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("competitors").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").get());
});
