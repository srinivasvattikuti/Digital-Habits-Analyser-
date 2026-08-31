/**
 * Server-Driven UI (SDUI) Customization Backend Endpoint
 * 
 * Express + Google Gemini API implementation with Structured Outputs / JSON Schema validation.
 * 
 * Endpoints:
 *  POST /api/v1/dashboard/customize  -> Processes user natural language prompt into dynamic UI layout JSON
 *  GET  /api/v1/dashboard/layout/:userId -> Fetches user's saved layout configuration
 *  POST /api/v1/dashboard/layout/:userId -> Persists user layout configuration
 */

const express = require('express');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 8080;

app.use(cors());
app.use(express.json());

// In-Memory Database Store indexed by user_id
const userLayoutsDatabase = new Map();

// Strict Component Types allowable by the native Android client
const ALLOWABLE_CARD_TYPES = [
  'HERO_USAGE',
  'SUMMARY_METRICS',
  'PROACTIVE_NUDGES',
  'HOURLY_HEATMAP',
  'GOALS_TRACKER',
  'WEEKLY_TRENDS',
  'NOTIFICATION_LEADERBOARD',
  'WEEK_OVER_WEEK',
  'BEHAVIOR_FORECAST',
  'RADAR_DIMENSIONS',
  'CATEGORY_DISTRIBUTION',
  'COMPULSIVE_GAUGE',
  'TOP_APPS',
  'USAGE_OVER_TIME',
  'RECOMMENDATION_BANNER',
  'AI_INSIGHT_BANNER'
];

// Strict JSON Schema for Gemini Model Output
const DASHBOARD_CUSTOMIZE_JSON_SCHEMA = {
  type: "object",
  properties: {
    requiresNativeUpdate: {
      type: "boolean",
      description: "True ONLY if the user request requires unsupported native device capabilities (e.g. installing binaries, modifying OS root/kernel permissions, bypassing Android system dialogs)."
    },
    explanation: {
      type: "string",
      description: "Concise 1-2 sentence explanation of why the layout was organized this way."
    },
    layout: {
      type: "object",
      properties: {
        layoutId: { type: "string" },
        layoutName: { type: "string" },
        description: { type: "string" },
        themeColor: { 
          type: "string", 
          enum: ["DEFAULT", "INDIGO", "EMERALD", "MIDNIGHT", "AMBER", "ROSE", "CRIMSON"] 
        },
        density: { 
          type: "string", 
          enum: ["COMPACT", "COMFORTABLE", "SPACIOUS"] 
        },
        headerTitle: { type: "string" },
        headerSubtitle: { type: "string" },
        components: {
          type: "array",
          items: {
            type: "object",
            properties: {
              id: { type: "string" },
              type: { 
                type: "string", 
                enum: ALLOWABLE_CARD_TYPES 
              },
              position: { type: "integer" },
              visible: { type: "boolean" },
              title: { type: "string" },
              cardStyle: { 
                type: "string", 
                enum: ["CARD", "MINIMAL", "HIGHLIGHT", "COMPACT", "OUTLINED"] 
              },
              parameters: {
                type: "object",
                properties: {
                  showScreenTime: { type: "boolean" },
                  showOpens: { type: "boolean" },
                  showCompulsiveRatio: { type: "boolean" },
                  showNotifications: { type: "boolean" },
                  showSteps: { type: "boolean" },
                  maxAppsCount: { type: "integer" },
                  layoutStyle: { type: "string", enum: ["GRID_2X2", "HORIZONTAL_ROW", "COMPACT_LIST"] },
                  highlightMetric: { type: "string" }
                },
                required: ["showScreenTime", "showOpens", "showCompulsiveRatio", "showNotifications"]
              }
            },
            required: ["id", "type", "position", "visible"]
          }
        }
      },
      required: ["layoutId", "layoutName", "themeColor", "density", "components"]
    }
  },
  required: ["requiresNativeUpdate", "explanation"]
};

/**
 * Checks for unsupported native device requests
 */
function checkUnsupportedNativeCapabilities(prompt) {
  const lower = (prompt || '').toLowerCase();
  const forbiddenPatterns = [
    'install apk', 'download apk', 'install binary', 'compile code',
    'root permission', 'grant root', 'hack', 'modify os',
    'bypass permission', 'system permission without asking', 'kernel',
    'execute shell', 'run bash', 'install native library', 'load so library',
    'access private database of other app', 'intercept banking password',
    'hardware override', 'flash rom', 'keylogger'
  ];

  for (const pattern of forbiddenPatterns) {
    if (lower.includes(pattern)) {
      return "This customization requires a native app update and cannot be rendered dynamically.";
    }
  }
  return null;
}

/**
 * POST /api/v1/dashboard/customize
 * Translates user natural language prompt into dynamic UI layout JSON via Gemini.
 */
app.post('/api/v1/dashboard/customize', async (req, res) => {
  try {
    const { userPrompt, userId = 'current_user', currentLayout, telemetryContext } = req.body;

    if (!userPrompt || typeof userPrompt !== 'string') {
      return res.status(400).json({
        success: false,
        errorMessage: 'User prompt is required.'
      });
    }

    // Step 1: Pre-flight security guardrail check
    const securityRejection = checkUnsupportedNativeCapabilities(userPrompt);
    if (securityRejection) {
      return res.status(200).json({
        success: false,
        layout: null,
        explanation: 'The requested operation requires low-level Android native system capabilities.',
        errorMessage: securityRejection,
        requiresNativeUpdate: true
      });
    }

    const apiKey = process.env.GEMINI_API_KEY || process.env.API_KEY;
    if (!apiKey) {
      // Return rule-based fallback response
      const fallbackLayout = generateRuleBasedLayout(userPrompt, userId, currentLayout);
      return res.json({
        success: true,
        layout: fallbackLayout,
        explanation: `Customized layout based on prompt: "${userPrompt}" (Rule-Based Server Engine)`,
        requiresNativeUpdate: false
      });
    }

    // Step 2: Call Gemini API with Structured Outputs (responseSchema)
    const systemInstruction = `
      You are the Server-Driven UI (SDUI) Customization Engine for an Android Digital Wellness App.
      Translate the user's natural language customization request into a valid UI layout JSON matching the strict schema.

      CRITICAL SECURITY GUARDRAILS:
      - If the user asks for unsupported native capabilities (e.g., installing external APK binaries, hacking root permissions, modifying OS kernel, bypassing Android permission dialogs), set requiresNativeUpdate to true.

      ALLOWABLE COMPONENTS:
      ${ALLOWABLE_CARD_TYPES.join(', ')}
    `;

    const userContent = `
      USER PROMPT: "${userPrompt}"
      
      TELEMETRY CONTEXT:
      - User: ${telemetryContext?.userName || 'User'} (${telemetryContext?.userRole || 'Professional'})
      - Screen Time: ${telemetryContext?.totalScreenTimeMinutes || 0} mins
      - Total Opens: ${telemetryContext?.totalOpens || 0}
      - Notifications: ${telemetryContext?.totalNotifications || 0}
      - Compulsive Ratio: ${telemetryContext?.compulsiveScore || 50}%
      - Top Apps: ${(telemetryContext?.topApps || []).join(', ')}
      - Top Categories: ${(telemetryContext?.topCategories || []).join(', ')}
      - Late Night Active: ${telemetryContext?.hasLateNightUsage ? 'YES' : 'NO'}

      Generate the optimal UI configuration JSON.
    `;

    const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`;
    
    const requestBody = {
      contents: [{ role: 'user', parts: [{ text: userContent }] }],
      systemInstruction: { parts: [{ text: systemInstruction }] },
      generationConfig: {
        temperature: 0.2,
        responseMimeType: "application/json",
        responseSchema: DASHBOARD_CUSTOMIZE_JSON_SCHEMA
      }
    };

    const response = await fetch(geminiUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestBody)
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error('Gemini API Error:', errorText);
      const fallbackLayout = generateRuleBasedLayout(userPrompt, userId, currentLayout);
      return res.json({
        success: true,
        layout: fallbackLayout,
        explanation: `Customized layout based on prompt: "${userPrompt}" (Fallback)`,
        requiresNativeUpdate: false
      });
    }

    const data = await response.json();
    const rawText = data.candidates?.[0]?.content?.parts?.[0]?.text;
    const parsed = JSON.parse(rawText);

    if (parsed.requiresNativeUpdate) {
      return res.json({
        success: false,
        layout: null,
        explanation: parsed.explanation || 'Native update required.',
        errorMessage: "This customization requires a native app update and cannot be rendered dynamically.",
        requiresNativeUpdate: true
      });
    }

    // Save to user database
    if (parsed.layout) {
      parsed.layout.userId = userId;
      parsed.layout.generatedFromPrompt = userPrompt;
      userLayoutsDatabase.set(userId, parsed.layout);
    }

    return res.json({
      success: true,
      layout: parsed.layout,
      explanation: parsed.explanation || 'Layout customized successfully.',
      requiresNativeUpdate: false
    });

  } catch (error) {
    console.error('Server error in /api/v1/dashboard/customize:', error);
    res.status(500).json({
      success: false,
      errorMessage: error.message || 'Internal Server Error'
    });
  }
});

/**
 * GET /api/v1/dashboard/layout/:userId
 * Retrieves the stored personalized layout for a user.
 */
app.get('/api/v1/dashboard/layout/:userId', (req, res) => {
  const { userId } = req.params;
  const layout = userLayoutsDatabase.get(userId);

  if (!layout) {
    return res.json({
      success: true,
      layout: null,
      message: 'No custom layout saved for user; client should use default.'
    });
  }

  res.json({
    success: true,
    layout: layout
  });
});

/**
 * POST /api/v1/dashboard/layout/:userId
 * Saves or updates a user-specific layout JSON.
 */
app.post('/api/v1/dashboard/layout/:userId', (req, res) => {
  const { userId } = req.params;
  const { layout } = req.body;

  if (!layout) {
    return res.status(400).json({ success: false, errorMessage: 'Layout payload is required.' });
  }

  layout.userId = userId;
  layout.timestamp = Date.now();
  userLayoutsDatabase.set(userId, layout);

  res.json({
    success: true,
    message: 'Layout persisted successfully.',
    layout: layout
  });
});

/**
 * Fallback rule-based layout generator
 */
function generateRuleBasedLayout(prompt, userId, currentLayout) {
  const lower = (prompt || '').toLowerCase();
  const wantsScreenTimeTop = lower.includes('screen time') || lower.includes('usage');
  const wantsHideNotifications = lower.includes('hide notification') || lower.includes('no notifications');
  const wantsSimplify = lower.includes('simplify') || lower.includes('minimal');

  const components = [
    {
      id: 'hero_usage',
      type: 'HERO_USAGE',
      position: wantsScreenTimeTop ? 1 : 2,
      visible: true,
      title: 'Daily Usage',
      cardStyle: 'HIGHLIGHT'
    },
    {
      id: 'summary_metrics',
      type: 'SUMMARY_METRICS',
      position: wantsScreenTimeTop ? 2 : 1,
      visible: true,
      title: 'Key Metrics',
      parameters: {
        showScreenTime: true,
        showOpens: true,
        showCompulsiveRatio: !wantsSimplify,
        showNotifications: !wantsHideNotifications,
        showSteps: true,
        maxAppsCount: 8,
        layoutStyle: wantsSimplify ? 'COMPACT_LIST' : 'GRID_2X2'
      }
    },
    {
      id: 'goals_tracker',
      type: 'GOALS_TRACKER',
      position: 3,
      visible: true,
      title: 'Habit Goals & Limits'
    },
    {
      id: 'proactive_nudges',
      type: 'PROACTIVE_NUDGES',
      position: 4,
      visible: true,
      title: 'Proactive Nudges'
    },
    {
      id: 'hourly_heatmap',
      type: 'HOURLY_HEATMAP',
      position: 5,
      visible: !wantsSimplify,
      title: '24-Hour Usage Heatmap'
    },
    {
      id: 'notif_leaderboard',
      type: 'NOTIFICATION_LEADERBOARD',
      position: 6,
      visible: !wantsHideNotifications && !wantsSimplify,
      title: 'Interruption Leaderboard'
    },
    {
      id: 'top_apps',
      type: 'TOP_APPS',
      position: 7,
      visible: true,
      title: 'Top Apps',
      parameters: { maxAppsCount: wantsSimplify ? 4 : 8 }
    }
  ];

  return {
    layoutId: `custom_${Date.now()}`,
    userId: userId,
    layoutName: wantsSimplify ? 'Simplified Focus Layout' : 'Custom Overview',
    description: `Configured dynamically for: "${prompt}"`,
    themeColor: lower.includes('dark') || lower.includes('midnight') ? 'MIDNIGHT' : 'DEFAULT',
    density: wantsSimplify ? 'COMPACT' : 'COMFORTABLE',
    headerTitle: 'Dashboard',
    headerSubtitle: 'HABIT INSIGHTS',
    components: components,
    generatedFromPrompt: prompt,
    timestamp: Date.now()
  };
}

app.listen(PORT, () => {
  console.log(`HabitFlow SDUI Backend server listening on port ${PORT}`);
});
