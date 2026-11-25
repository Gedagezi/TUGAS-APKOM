package com.example.data

import com.example.model.AudiencePersona
import com.example.model.Campaign
import com.example.model.CampaignStatus
import com.example.model.SegmaNotification
import com.example.model.TrendCategory
import com.example.model.TrendInsight
import com.example.model.TrendingEditorialItem
import com.example.model.ViralAudioTrack

object SegmaDemoCatalog {

    val initialDemoCampaigns: List<Campaign> = listOf(
        Campaign(
            id = "demo_bring_your_own_box",
            name = "Bring Your Own Box!",
            brandName = "Eits Sampah",
            handle = "@eitssampah",
            objective = "Raise campus eco-awareness around single-use food packaging among students",
            description = "Peer-to-peer satirical Reels and TikTok POV campaign encouraging students to bring reusable lunchboxes to campus canteens.",
            targetAudience = "Active university students aged 18-22 who dine at campus canteens and hang out in student plazas",
            audienceTags = listOf("Students 18-22", "Campus Hub", "Eco-Friendly"),
            platforms = listOf("Instagram", "TikTok"),
            duration = "14 Days",
            budget = "Organic + Micro Creator Collab",
            status = CampaignStatus.ONGOING.name,
            demoData = true,
            targetReach = 15000L,
            targetEr = 5.5,
            reach = 18450L,
            views = 24600L,
            likes = 980L,
            comments = 142L,
            shares = 96L,
            saves = 55L,
            engagementRate = 6.9,
            dataSourceLabel = "Demo Data • Simulated IG & TikTok Snapshot",
            aiOverview = "Peer-to-peer campus awareness campaign leveraging relatable humor instead of preachy messaging to normalize carrying reusable containers.",
            aiCommunicationStrategy = "Use light satire and relatable student pride ('No shame in bringing your own container') paired with interactive Story polls.",
            aiContentPillars = listOf("Campus POV Humor", "Canteen Hacks", "Eco Challenge"),
            aiUploadStrategy = "Post Reels & TikTok 4x/week between 19:00-21:00 local time when student engagement peaks.",
            aiContentCalendar = "Day 1: POV Canteen Hook Reel\nDay 3: Interactive Table Cleanliness Poll\nDay 5: Duet Challenge with Campus Creators\nDay 7: Weekly Impact Recap Carousel",
            aiCaptions = "Hook: \"Tell me you're an eco-pro without telling me...\"\nCaption: Bringing your own lunchbox isn't extra—it's main-character energy. Tag a friend who always forgets theirs! #TrashIsAnIssueToo",
            aiHashtags = listOf("#TrashIsAnIssueToo", "#CampusEcoLife", "#ClearYourTrace", "#ZeroWasteCampus"),
            aiAudioRecommendation = "Fast 2s beat-drop hook (Demo Sample Track: Indie Satire Beat)",
            aiNextActionRecommendation = "Performance is in the High Engagement tier (6.9% ER vs 5.5% target). Share velocity peaks between 19:00-21:00. Launch a Part 2 POV Reel within 3 days while this format momentum remains high."
        ),
        Campaign(
            id = "demo_zero_waste_campus_week",
            name = "Zero-Waste Hangout Week",
            brandName = "Eits Sampah",
            handle = "@eitssampah",
            objective = "Drive 500 student pledges for plastic-free campus hangouts",
            description = "Completed 7-day campus challenge featuring student creator collabs and before-after lounge transformations.",
            targetAudience = "Student organizations and creative communities aged 18-24",
            audienceTags = listOf("Gen Z 18-24", "Campus Clubs", "Sustainability"),
            platforms = listOf("Instagram", "TikTok"),
            duration = "7 Days",
            budget = "$120 Creator Boost",
            status = CampaignStatus.COMPLETED.name,
            demoData = true,
            targetReach = 12000L,
            targetEr = 4.8,
            reach = 14200L,
            views = 19800L,
            likes = 640L,
            comments = 98L,
            shares = 72L,
            saves = 44L,
            engagementRate = 6.0,
            dataSourceLabel = "Demo Data • Completed Campaign Archive",
            aiOverview = "7-day sprint focused on student hangout zones with daily Story stickers and creator takeovers.",
            aiCommunicationStrategy = "Community-first co-creation with recognizable campus micro-influencers.",
            aiContentPillars = listOf("Before-After Lounge", "Creator Takeover", "Pledge Counter"),
            aiUploadStrategy = "Daily Stories at 12:30 + Reels at 19:30.",
            aiContentCalendar = "Mon-Sun daily countdown reels and pledge stickers.",
            aiCaptions = "Day 7 wrapped: 540+ student pledges! Clean table, clear mind.",
            aiHashtags = listOf("#ZeroWasteHangout", "#PlasticFreeCampus", "#StudentImpact"),
            aiAudioRecommendation = "Upbeat transition drop (Demo Snapshot)",
            aiNextActionRecommendation = "Campaign completed above target (6.0% vs 4.8% ER). Ready for full AI Evaluation Report."
        ),
        Campaign(
            id = "demo_refill_station_launch",
            name = "Campus Refill Station Launch",
            brandName = "Gedagezhi Studio",
            handle = "@gedagezhi",
            objective = "Announce new campus tumbler refill stations with carousel guides",
            description = "Scheduled multi-slide carousel and short Reel teaser series showing refill station map locations.",
            targetAudience = "Commuter students and eco-conscious freshmen",
            audienceTags = listOf("Freshmen", "Commuters", "Eco-Utility"),
            platforms = listOf("Instagram"),
            duration = "10 Days",
            budget = "Organic",
            status = CampaignStatus.SCHEDULED.name,
            demoData = true,
            targetReach = 10000L,
            targetEr = 4.5,
            reach = 0L,
            views = 0L,
            likes = 0L,
            comments = 0L,
            shares = 0L,
            saves = 0L,
            engagementRate = 0.0,
            dataSourceLabel = "Scheduled • No live metrics yet",
            aiOverview = "Utility-driven launch campaign emphasizing hydration savings and campus map saves.",
            aiCommunicationStrategy = "Save-worthy visual map carousels paired with crisp 15s directional Reels.",
            aiContentPillars = listOf("Campus Map Guide", "Savings Calculator", "Tumbler Flex"),
            aiUploadStrategy = "Publish Carousel #1 Tuesday 18:30.",
            aiContentCalendar = "Week 1: Map Reveal + Savings Breakdown.",
            aiCaptions = "Save this post so you never pay for bottled water on campus again.",
            aiHashtags = listOf("#RefillNotLandfill", "#CampusMap", "#HydrateSmart"),
            aiAudioRecommendation = "Crisp minimal lo-fi beat (Demo Snapshot)",
            aiNextActionRecommendation = "Campaign is scheduled. Monitor initial saves during the first 24 hours after launch."
        )
    )

    val initialDemoPersona = AudiencePersona(
        id = "demo_persona_campus",
        name = "Campus Eco-Trendsetters",
        ageRange = "18-22",
        location = "Urban University Campuses",
        interests = listOf("Campus Humor", "Eco-Lifestyle", "Thrift & Coffee", "Short-Form Reels"),
        painPoints = "Ignores preachy, corporate-sounding sustainability ads; responds to peer validation and witty, low-effort daily habits.",
        preferredFormats = listOf("15-20s POV Reels", "TikTok Satire", "Interactive Story Polls"),
        toneOfVoice = "Casual, witty, peer-to-peer, non-preachy",
        feasibilityScore = 88L,
        summary = "High local affinity with low content saturation for humorous peer-led eco challenges."
    )

    val trendInsights: List<TrendInsight> = listOf(
        TrendInsight(
            id = "trend_eco_lifestyle",
            title = "#TrashIsAnIssueToo & Eco-Lifestyle Education",
            category = TrendCategory.NICHE_TOPICS,
            growthLabel = "+285% Search (Demo)",
            peakTimeNote = "Peak engagement occurs between 19:00-21:00 local time in the university student segment.",
            chartPoints = listOf(0.24f, 0.38f, 0.52f, 0.68f, 0.84f, 0.96f),
            topHashtags = listOf(
                "#TrashIsAnIssueToo" to "48.2K posts ↗ +310%",
                "#ClearYourTrace" to "19.5K posts ↗ +195%",
                "#PlasticFreeCampus" to "12.1K posts ↗ +140%"
            ),
            opportunityTitle = "Trend Opportunity from Segma AI",
            opportunityBody = "The 'Zero-Waste Hangout' topic is up 180% among urban students in demo trend tracking, yet few competitors pair it with quick satirical hook audio."
        ),
        TrendInsight(
            id = "trend_hashtags_weekly",
            title = "Community Eco-Challenge Hashtags",
            category = TrendCategory.HASHTAGS,
            growthLabel = "+215% Mentions (Demo)",
            peakTimeNote = "Highest comment velocity occurs right after lunch hours (12:30-14:00) and evening wind-down (20:00).",
            chartPoints = listOf(0.30f, 0.36f, 0.48f, 0.62f, 0.78f, 0.90f),
            topHashtags = listOf(
                "#BringYourOwnBox" to "31.4K posts ↗ +245%",
                "#CampusGreenChallenge" to "15.8K posts ↗ +175%",
                "#EcoCheck" to "27.0K posts ↗ +132%"
            ),
            opportunityTitle = "Low-Competition Hashtag Cluster",
            opportunityBody = "Combining #BringYourOwnBox with hyper-local campus tags increases discoverability in Explore feeds without competing against global brand ads."
        ),
        TrendInsight(
            id = "trend_audio_hooks",
            title = "2-Second Beat-Drop & Lyric Satire Audio",
            category = TrendCategory.MUSIC_AUDIO,
            growthLabel = "+230% Shares (Demo)",
            peakTimeNote = "Audio hooks entering within the first 2.0 seconds retain 34% more viewers past second 5.",
            chartPoints = listOf(0.20f, 0.41f, 0.55f, 0.70f, 0.88f, 0.95f),
            topHashtags = listOf(
                "#BackburnerEdit" to "64.0K reels ↗ +260%",
                "#DropDeadTransition" to "42.3K reels ↗ +190%",
                "#HookFirstAudio" to "18.9K reels ↗ +155%"
            ),
            opportunityTitle = "Audio Timing Advantage",
            opportunityBody = "Aligning a before-and-after scene cut right on the 2-second beat drop triggers replay loops and higher share-to-reach ratios."
        ),
        TrendInsight(
            id = "trend_formats_pov",
            title = "15s Before-vs-After POV & Story Poll Loops",
            category = TrendCategory.FORMATS,
            growthLabel = "+170% Saves (Demo)",
            peakTimeNote = "Short 15-20s POV clips paired with a follow-up Story poll outperform static flyers by 2.4x in demo benchmarks.",
            chartPoints = listOf(0.28f, 0.39f, 0.49f, 0.60f, 0.75f, 0.87f),
            topHashtags = listOf(
                "#POVTransition" to "52.1K posts ↗ +210%",
                "#CanteenPOV" to "9.4K posts ↗ +185%",
                "#ReelCarouselCombo" to "14.2K posts ↗ +128%"
            ),
            opportunityTitle = "Format Gap in Niche Brands",
            opportunityBody = "Most sustainability accounts still rely on text-heavy static slides. Switching to snappy 15s POV Reels captures organic reach."
        )
    )

    val trendingEditorials: List<TrendingEditorialItem> = listOf(
        TrendingEditorialItem(
            id = "editorial_top3_audio",
            badgeTitle = "TOP 3\nMost Viral\nAudio",
            headline = "Top 3 Trending Audio Picks to Boost Campaign Engagement",
            updatedLabel = "Demo Snapshot • Curated by Segma AI",
            readTimeLabel = "2 min read",
            categoryTags = listOf("TREND RADAR", "VIRAL AUDIO", "DEMO DATA"),
            tracks = listOf(
                ViralAudioTrack(
                    rank = 1,
                    title = "Backburner",
                    artist = "Niki",
                    hookTiming = "Lyric hook in first 2s",
                    aiUsageTip = "Great for playful irony—compare putting a good habit on the 'backburner'."
                ),
                ViralAudioTrack(
                    rank = 2,
                    title = "Drop Dead",
                    artist = "Olivia Rodrigo",
                    hookTiming = "Sharp beat-drop cut",
                    aiUsageTip = "Ideal for snappy before-and-after visual transitions right when the beat hits."
                ),
                ViralAudioTrack(
                    rank = 3,
                    title = "What If I Call",
                    artist = "Alex Crichton",
                    hookTiming = "Reflective 3s opener",
                    aiUsageTip = "Pair with a question text-overlay in the first 3 seconds to spark comment debates."
                )
            ),
            whyTrending = "Notice: Because no live TikTok/Instagram audio chart API is connected yet, this list is presented as a curated Demo Snapshot. Audio patterns with an immediate hook inside the first 2 seconds and a decisive beat-drop encourage viewers to watch through the transition, improving retention and replay velocity in short-form feeds.",
            responsiveSegments = "In demo campaign modeling, ages 18-24 (Gen Z & active university students) show the highest share propensity (up to 2.3x baseline) for casual, slightly satirical storytelling that feels peer-made rather than like a rigid advertisement.",
            aiRecommendations = listOf(
                "For Gentle Satire Content: Use #1 (Backburner) to playfully call out relatable habits—e.g. \"Don't treat campus cleanliness like a backburner when you're rushing between classes.\"",
                "For Fast Before-After Transitions: Use #2 (Drop Dead) and sync your scene switch cleanly to the full instrumental entry.",
                "For Call-to-Action & Comments: Use #3 (What If I Call) with a bold reflective question overlay in the first 3 seconds to invite audience replies."
            )
        ),
        TrendingEditorialItem(
            id = "editorial_hashtag_week",
            badgeTitle = "TOP\nWeekly\nHashtags",
            headline = "Micro-Community Hashtags Outperforming Broad Viral Tags",
            updatedLabel = "Demo Snapshot • Curated by Segma AI",
            readTimeLabel = "2 min read",
            categoryTags = listOf("HASHTAGS", "DISCOVERABILITY", "DEMO DATA"),
            tracks = listOf(
                ViralAudioTrack(1, "#TrashIsAnIssueToo", "48.2K demo posts", "+310% velocity", "Anchor tag for campus eco storytelling"),
                ViralAudioTrack(2, "#ClearYourTrace", "19.5K demo posts", "+195% velocity", "Action-oriented CTA tag"),
                ViralAudioTrack(3, "#ZeroWasteHangout", "12.1K demo posts", "+140% velocity", "Lifestyle angle tag")
            ),
            whyTrending = "This Demo Snapshot illustrates how pairing one broad movement tag with two hyper-specific niche tags helps social algorithms match content to high-intent local communities rather than low-retention generic audiences.",
            responsiveSegments = "College creators, student clubs, and eco-conscious Gen Z shoppers who actively save practical tips and share relatable memes in group chats.",
            aiRecommendations = listOf(
                "Use 3-5 focused hashtags per post instead of 20+ generic tags.",
                "Place one campaign-branded tag consistently across Reels and Carousels to build series recognition."
            )
        )
    )

    val notifications: List<SegmaNotification> = listOf(
        SegmaNotification(
            id = "notif_1",
            title = "High Engagement Momentum (+24% vs Target)",
            body = "Demo campaign 'Bring Your Own Box!' reached 6.9% ER. Ideal window to post Part 2 within 72 hours.",
            severity = "OPPORTUNITY",
            timestampLabel = "2h ago"
        ),
        SegmaNotification(
            id = "notif_2",
            title = "Peak Audience Window Starting Soon",
            body = "Campus student segment activity typically peaks between 19:00 and 21:00. Prepare your scheduled Reel.",
            severity = "REMINDER",
            timestampLabel = "4h ago"
        ),
        SegmaNotification(
            id = "notif_3",
            title = "Campaign Ready for AI Evaluation",
            body = "'Zero-Waste Hangout Week' is marked Completed. Generate an AI Evaluation Report to archive learnings.",
            severity = "ACTION",
            timestampLabel = "1d ago"
        )
    )
}
