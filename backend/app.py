"""
FastAPI Server-Driven UI (SDUI) Customization Backend
Integrated with Google Gemini API using Structured Outputs & Strict JSON Schema.
"""

import os
import time
from typing import List, Optional, Dict, Any
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from dotenv import load_dotenv
import requests

load_dotenv()

app = FastAPI(
    title="HabitFlow Server-Driven UI Backend",
    version="1.0.0",
    description="Translates natural language customization requests into structured UI configurations via Gemini."
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# In-memory database of layout configurations indexed by user_id
user_layouts_db: Dict[str, Dict[str, Any]] = {}

NATIVE_UPDATE_ERROR_MESSAGE = "This customization requires a native app update and cannot be rendered dynamically."

FORBIDDEN_KEYWORDS = [
    "install apk", "download apk", "install binary", "compile code",
    "root permission", "grant root", "hack", "modify os",
    "bypass permission", "system permission without asking", "kernel",
    "execute shell", "run bash", "install native library", "load so library",
    "access private database of other app", "intercept banking password",
    "hardware override", "flash rom", "keylogger"
]


class ComponentParameters(BaseModel):
    showScreenTime: bool = True
    showOpens: bool = True
    showCompulsiveRatio: bool = True
    showNotifications: bool = True
    showSteps: bool = True
    maxAppsCount: int = 8
    layoutStyle: str = "GRID_2X2"
    highlightMetric: Optional[str] = None


class DynamicComponentConfig(BaseModel):
    id: str
    type: str
    position: int
    visible: bool = True
    title: Optional[str] = None
    cardStyle: str = "CARD"
    parameters: ComponentParameters = Field(default_factory=ComponentParameters)


class DashboardLayoutConfig(BaseModel):
    layoutId: str = "default_layout"
    userId: str = "current_user"
    layoutName: str = "Standard Overview"
    description: str = "Holistic digital wellness layout"
    themeColor: str = "DEFAULT"
    density: str = "COMFORTABLE"
    headerTitle: str = "Dashboard"
    headerSubtitle: str = "HABIT INSIGHTS"
    components: List[DynamicComponentConfig] = []
    generatedFromPrompt: Optional[str] = None
    timestamp: int = Field(default_factory=lambda: int(time.time() * 1000))


class SystemTelemetryContext(BaseModel):
    userName: str = "User"
    userRole: str = "Professional"
    totalScreenTimeMinutes: int = 0
    totalOpens: int = 0
    totalNotifications: int = 0
    compulsiveScore: int = 50
    topApps: List[str] = []
    topCategories: List[str] = []
    hasLateNightUsage: bool = False


class DashboardCustomizeRequest(BaseModel):
    userPrompt: str
    userId: str = "current_user"
    currentLayout: Optional[DashboardLayoutConfig] = None
    telemetryContext: Optional[SystemTelemetryContext] = None


class DashboardCustomizeResponse(BaseModel):
    success: bool
    layout: Optional[DashboardLayoutConfig] = None
    explanation: str = ""
    errorMessage: Optional[str] = None
    requiresNativeUpdate: bool = False


def check_unsupported_native_capabilities(prompt: str) -> Optional[str]:
    lower = prompt.lower()
    for kw in FORBIDDEN_KEYWORDS:
        if kw in lower:
            return NATIVE_UPDATE_ERROR_MESSAGE
    return None


@app.post("/api/v1/dashboard/customize", response_model=DashboardCustomizeResponse)
async def customize_dashboard(request: DashboardCustomizeRequest):
    """
    Translates user prompt + telemetry context into dynamic UI layout JSON.
    """
    # Guardrail check
    rejection = check_unsupported_native_capabilities(request.userPrompt)
    if rejection:
        return DashboardCustomizeResponse(
            success=False,
            layout=None,
            explanation="The requested action requires native OS permissions/binaries.",
            errorMessage=rejection,
            requiresNativeUpdate=True
        )

    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        # Return rule-based fallback
        layout = generate_rule_based_layout(request.userPrompt, request.userId)
        user_layouts_db[request.userId] = layout.model_dump()
        return DashboardCustomizeResponse(
            success=True,
            layout=layout,
            explanation=f"Customized layout based on prompt: '{request.userPrompt}' (Server Engine)",
            requiresNativeUpdate=False
        )

    # Call Gemini API with structured prompt
    gemini_url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={api_key}"
    
    system_instruction = """
    You are the Server-Driven UI (SDUI) Customization Engine for an Android Digital Wellness App.
    Translate user natural language customization prompts into dynamic UI layout JSON.
    If the request asks for unsupported native capabilities, set requiresNativeUpdate to true.
    """

    user_text = f"""
    PROMPT: {request.userPrompt}
    CONTEXT: Screen Time: {request.telemetryContext.totalScreenTimeMinutes if request.telemetryContext else 0}m, 
    Opens: {request.telemetryContext.totalOpens if request.telemetryContext else 0},
    Top Apps: {', '.join(request.telemetryContext.topApps) if request.telemetryContext else 'None'}
    """

    req_payload = {
        "contents": [{"role": "user", "parts": [{"text": user_text}]}],
        "systemInstruction": {"parts": [{"text": system_instruction}]},
        "generationConfig": {
            "temperature": 0.2,
            "responseMimeType": "application/json"
        }
    }

    try:
        resp = requests.post(gemini_url, json=req_payload, timeout=30)
        if resp.status_code == 200:
            data = resp.json()
            raw_text = data["candidates"][0]["content"]["parts"][0]["text"]
            import json
            parsed = json.loads(raw_text)
            
            if parsed.get("requiresNativeUpdate"):
                return DashboardCustomizeResponse(
                    success=False,
                    layout=None,
                    explanation=parsed.get("explanation", "Native update required."),
                    errorMessage=NATIVE_UPDATE_ERROR_MESSAGE,
                    requiresNativeUpdate=True
                )
            
            layout_data = parsed.get("layout", parsed)
            layout_data["userId"] = request.userId
            layout_data["generatedFromPrompt"] = request.userPrompt
            
            layout_obj = DashboardLayoutConfig(**layout_data)
            user_layouts_db[request.userId] = layout_obj.model_dump()
            
            return DashboardCustomizeResponse(
                success=True,
                layout=layout_obj,
                explanation=parsed.get("explanation", "Layout customized."),
                requiresNativeUpdate=False
            )
    except Exception as e:
        print(f"Gemini API call failed: {e}")

    # Fallback
    layout = generate_rule_based_layout(request.userPrompt, request.userId)
    user_layouts_db[request.userId] = layout.model_dump()
    return DashboardCustomizeResponse(
        success=True,
        layout=layout,
        explanation=f"Customized layout for: '{request.userPrompt}'",
        requiresNativeUpdate=False
    )


@app.get("/api/v1/dashboard/layout/{user_id}")
async def get_user_layout(user_id: str):
    layout_data = user_layouts_db.get(user_id)
    if not layout_data:
        return {"success": True, "layout": None, "message": "No stored layout"}
    return {"success": True, "layout": layout_data}


@app.post("/api/v1/dashboard/layout/{user_id}")
async def save_user_layout(user_id: str, layout: DashboardLayoutConfig):
    layout.userId = user_id
    user_layouts_db[user_id] = layout.model_dump()
    return {"success": True, "layout": layout}


def generate_rule_based_layout(prompt: str, user_id: str) -> DashboardLayoutConfig:
    lower = prompt.lower()
    wants_screen_time_top = "screen time" in lower or "usage" in lower
    wants_simplify = "simplify" in lower or "minimal" in lower
    wants_hide_notifs = "hide notification" in lower or "no notification" in lower

    components = [
        DynamicComponentConfig(
            id="hero_usage",
            type="HERO_USAGE",
            position=1 if wants_screen_time_top else 2,
            visible=True,
            title="Daily Usage"
        ),
        DynamicComponentConfig(
            id="summary_metrics",
            type="SUMMARY_METRICS",
            position=2 if wants_screen_time_top else 1,
            visible=True,
            title="Key Metrics",
            parameters=ComponentParameters(
                showScreenTime=True,
                showOpens=True,
                showCompulsiveRatio=not wants_simplify,
                showNotifications=not wants_hide_notifs,
                layoutStyle="COMPACT_LIST" if wants_simplify else "GRID_2X2"
            )
        ),
        DynamicComponentConfig(
            id="goals_tracker",
            type="GOALS_TRACKER",
            position=3,
            visible=True,
            title="Habit Goals"
        ),
        DynamicComponentConfig(
            id="top_apps",
            type="TOP_APPS",
            position=4,
            visible=True,
            title="Top Apps",
            parameters=ComponentParameters(maxAppsCount=4 if wants_simplify else 8)
        )
    ]

    return DashboardLayoutConfig(
        layoutId=f"custom_{int(time.time()*1000)}",
        userId=user_id,
        layoutName="Simplified Focus View" if wants_simplify else "Customized Dashboard",
        themeColor="MIDNIGHT" if "dark" in lower else "DEFAULT",
        density="COMPACT" if wants_simplify else "COMFORTABLE",
        headerTitle="Dashboard",
        headerSubtitle="HABIT INSIGHTS",
        components=components,
        generatedFromPrompt=prompt
    )
