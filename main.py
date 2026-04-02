#!/usr/bin/env python3
"""
╔══════════════════════════════════════════════════════════════════════════════╗
║   NION ORCHESTRATION ENGINE  ·  aiNions Enterprise AI Platform              ║
║   Author  : aiNions Senior Engineering Team                                  ║
║   Version : 1.0.0                                                            ║
║                                                                              ║
║   Architecture                                                               ║
║     L1 Orchestrator  →  (Router Pattern)  →  Routing Plan                   ║
║     L2 Coordinators  →  (Domain Experts)  →  Agent Selection                ║
║     L3 Agents        →  (Specialists)     →  Task Execution                 ║
║     Cross-Cutting    →  (Shared Services) →  knowledge_retrieval + eval     ║
║                                                                              ║
║   Visibility Enforcement (Non-Negotiable)                                    ║
║     • L1 sees ONLY L2 domain names — never individual L3 agents             ║
║     • L2 sees ONLY its own L3 agents + cross-cutting agents                 ║
╚══════════════════════════════════════════════════════════════════════════════╝
"""

from __future__ import annotations

import json
import logging
import os
import sys
import textwrap
import time
from datetime import datetime
from enum import Enum
from typing import Any

from dotenv import load_dotenv
from pydantic import BaseModel, Field
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

# Load environment variables from .env if present.
# encoding='utf-8-sig' tolerates UTF-8 BOM and UTF-16 files created by Windows Notepad.
load_dotenv(encoding='utf-8-sig')


# ══════════════════════════════════════════════════════════════════════════════
# SECTION 1  ·  STRUCTURED LOGGING
# ══════════════════════════════════════════════════════════════════════════════

_LOG_FMT = "%(asctime)s | %(levelname)-8s | %(name)-38s | %(message)s"
_DATE_FMT = "%Y-%m-%d %H:%M:%S"

logging.basicConfig(
    level=logging.INFO,
    format=_LOG_FMT,
    datefmt=_DATE_FMT,
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler("nion_orchestration.log", mode="a", encoding="utf-8"),
    ],
)


def get_logger(name: str) -> logging.Logger:
    """Factory: returns a namespaced logger under the 'nion.*' hierarchy."""
    return logging.getLogger(f"nion.{name}")


# ══════════════════════════════════════════════════════════════════════════════
# SECTION 2  ·  ENUMS, CONSTANTS & VISIBILITY MAP
# ══════════════════════════════════════════════════════════════════════════════

class L2Domain(str, Enum):
    """The three L2 Coordinator domains."""
    TRACKING_EXECUTION          = "TRACKING_EXECUTION"
    COMMUNICATION_COLLABORATION = "COMMUNICATION_COLLABORATION"
    LEARNING_IMPROVEMENT        = "LEARNING_IMPROVEMENT"


class L3Agent(str, Enum):
    """All L3 Specialist Agents — grouped by owning L2 domain."""

    # ── TRACKING_EXECUTION ────────────────────────────────────────────────────
    ACTION_ITEM_EXTRACTION = "action_item_extraction"
    RISK_TRACKING          = "risk_tracking"
    DECISION_TRACKING      = "decision_tracking"
    PROGRESS_MONITORING    = "progress_monitoring"
    DEADLINE_MANAGEMENT    = "deadline_management"

    # ── COMMUNICATION_COLLABORATION ───────────────────────────────────────────
    MESSAGE_DELIVERY          = "message_delivery"
    STAKEHOLDER_NOTIFICATION  = "stakeholder_notification"
    MEETING_SUMMARY           = "meeting_summary"
    ESCALATION_HANDLER        = "escalation_handler"
    QNA                       = "qna"

    # ── LEARNING_IMPROVEMENT ──────────────────────────────────────────────────
    RETROSPECTIVE_ANALYSIS  = "retrospective_analysis"
    PATTERN_RECOGNITION     = "pattern_recognition"
    KNOWLEDGE_BASE_UPDATE   = "knowledge_base_update"
    IMPROVEMENT_SUGGESTIONS = "improvement_suggestions"

    # ── CROSS-CUTTING (shared across all L2 domains) ──────────────────────────
    KNOWLEDGE_RETRIEVAL = "knowledge_retrieval"
    EVALUATION          = "evaluation"


# ─────────────────────────────────────────────────────────────────────────────
#  VISIBILITY MAP  —  the single source of truth that enforces architecture.
#  L2 coordinators are ONLY allowed to invoke agents present in their slice.
# ─────────────────────────────────────────────────────────────────────────────
DOMAIN_AGENTS: dict[L2Domain, list[L3Agent]] = {
    L2Domain.TRACKING_EXECUTION: [
        L3Agent.ACTION_ITEM_EXTRACTION,
        L3Agent.RISK_TRACKING,
        L3Agent.DECISION_TRACKING,
        L3Agent.PROGRESS_MONITORING,
        L3Agent.DEADLINE_MANAGEMENT,
    ],
    L2Domain.COMMUNICATION_COLLABORATION: [
        L3Agent.MESSAGE_DELIVERY,
        L3Agent.STAKEHOLDER_NOTIFICATION,
        L3Agent.MEETING_SUMMARY,
        L3Agent.ESCALATION_HANDLER,
        L3Agent.QNA,
    ],
    L2Domain.LEARNING_IMPROVEMENT: [
        L3Agent.RETROSPECTIVE_ANALYSIS,
        L3Agent.PATTERN_RECOGNITION,
        L3Agent.KNOWLEDGE_BASE_UPDATE,
        L3Agent.IMPROVEMENT_SUGGESTIONS,
    ],
}

CROSS_CUTTING_AGENTS: list[L3Agent] = [
    L3Agent.KNOWLEDGE_RETRIEVAL,
    L3Agent.EVALUATION,
]

# Human-readable descriptions used in LLM prompts
AGENT_DESCRIPTIONS: dict[L3Agent, str] = {
    L3Agent.ACTION_ITEM_EXTRACTION:   "Extract concrete tasks with owners and due dates",
    L3Agent.RISK_TRACKING:            "Identify, score, and suggest mitigations for project risks",
    L3Agent.DECISION_TRACKING:        "Capture decisions made, their rationale, and owners",
    L3Agent.PROGRESS_MONITORING:      "Summarise current progress against plan or sprint goal",
    L3Agent.DEADLINE_MANAGEMENT:      "Flag overdue or at-risk deadlines",
    L3Agent.MESSAGE_DELIVERY:         "Draft and route messages to appropriate recipients",
    L3Agent.STAKEHOLDER_NOTIFICATION: "Compose stakeholder status update notifications",
    L3Agent.MEETING_SUMMARY:          "Generate structured meeting minutes from transcripts",
    L3Agent.ESCALATION_HANDLER:       "Determine escalation path, priority level, and SLA",
    L3Agent.QNA:                      "Answer project-related questions from available context",
    L3Agent.RETROSPECTIVE_ANALYSIS:   "Analyse past sprint or project phase performance",
    L3Agent.PATTERN_RECOGNITION:      "Detect recurring themes, blockers, or failure patterns",
    L3Agent.KNOWLEDGE_BASE_UPDATE:    "Identify and prepare updates to the project knowledge base",
    L3Agent.IMPROVEMENT_SUGGESTIONS:  "Recommend process or technical improvements",
    L3Agent.KNOWLEDGE_RETRIEVAL:      "Retrieve relevant project context, history, and metadata",
    L3Agent.EVALUATION:               "Evaluate tone, accuracy, and completeness of AI outputs",
}


# ══════════════════════════════════════════════════════════════════════════════
# SECTION 3  ·  PYDANTIC DATA MODELS
# ══════════════════════════════════════════════════════════════════════════════

class PlannedTask(BaseModel):
    """A single routing task created by L1 for one L2 domain."""
    task_id:     str       = Field(description="Unique task identifier, e.g. TASK-001")
    domain:      str       = Field(description="Target L2 domain name")
    rationale:   str       = Field(description="Why this domain is needed for this message")
    agents_hint: list[str] = Field(description="Suggested L3 agent names (hints only — L2 decides)")
    priority:    str       = Field(description="HIGH | MEDIUM | LOW")


class L1Plan(BaseModel):
    """Structured plan produced by the L1 Orchestrator."""
    intent:               str             = Field(description="Detected intent in one sentence")
    message_type:         str             = Field(description="status_query|feasibility|decision_request|meeting_transcript|escalation|ambiguous")
    gaps:                 list[str]       = Field(description="Missing context or ambiguities")
    tasks:                list[PlannedTask] = Field(description="Ordered routing tasks")
    cross_cutting_needed: list[str]       = Field(description="Cross-cutting agents required")


class L3AgentResult(BaseModel):
    """Result returned by a single L3 agent execution."""
    agent:        str  = Field(description="Agent identifier")
    task_id:      str  = Field(description="Parent task ID from L1 plan")
    status:       str  = Field(description="SUCCESS | PARTIAL | FAILED")
    output:       Any  = Field(description="Agent output payload (structured JSON)")
    exec_time_ms: int  = Field(description="Execution time in milliseconds")


class L2DomainResult(BaseModel):
    """Aggregated result for one L2 domain coordinator execution."""
    domain:          str                  = Field(description="L2 domain name")
    task_id:         str                  = Field(description="Matching L1 task ID")
    agents_invoked:  list[str]            = Field(description="L3 agents that were run")
    results:         list[L3AgentResult]  = Field(description="Per-agent results")
    summary:         str                  = Field(description="Domain-level human summary")


class CrossCuttingResult(BaseModel):
    """Result from a cross-cutting agent."""
    agent:  str = Field(description="Agent name")
    status: str = Field(description="SUCCESS | PARTIAL | FAILED")
    output: Any = Field(description="Agent output payload")


class OrchestrationResult(BaseModel):
    """The complete output of one NION orchestration run."""
    message_id:              str                       = Field(description="Unique run ID")
    timestamp:               str                       = Field(description="ISO-8601 timestamp")
    l1_plan:                 L1Plan                    = Field(description="L1 routing plan")
    l2_results:              list[L2DomainResult]      = Field(description="All L2 execution results")
    cross_cutting_results:   list[CrossCuttingResult]  = Field(description="Cross-cutting results")


# ══════════════════════════════════════════════════════════════════════════════
# SECTION 4  ·  LLM FACTORY
# ══════════════════════════════════════════════════════════════════════════════

def build_llm(temperature: float = 0.2):
    """
    Returns a LangChain chat model based on the LLM_PROVIDER environment variable.

    Supported values (set LLM_PROVIDER in your .env):
      nvidia  → NVIDIA NIM API  (DEFAULT — free tier, no credit card required)
      openai  → OpenAI API
      gemini  → Google Gemini API

    ── NVIDIA NIM setup (recommended) ────────────────────────────────────────
      1. Get a free key at: https://build.nvidia.com  (click any model → Get API Key)
      2. In your .env:
             LLM_PROVIDER = nvidia
             NVIDIA_API_KEY = nvapi-xxxxxxxxxxxxxxxxxxxx
      Optional model override:
             NVIDIA_MODEL = meta/llama-3.3-70b-instruct   ← default
      Other good free models:
             nvidia/llama-3.1-nemotron-70b-instruct
             mistralai/mistral-large-2-instruct
             microsoft/phi-3-medium-128k-instruct

    ── Other providers ────────────────────────────────────────────────────────
      GOOGLE_API_KEY = <your Gemini key>    (when LLM_PROVIDER=gemini)
      OPENAI_API_KEY = <your OpenAI key>    (when LLM_PROVIDER=openai)
    """
    provider = os.getenv("LLM_PROVIDER", "nvidia").lower()
    log = get_logger("llm_factory")

    # ── NVIDIA NIM  (default — official langchain-nvidia-ai-endpoints) ──────────
    # Primary: ChatNVIDIA from the official NVIDIA LangChain integration package.
    # Fallback: ChatOpenAI + NVIDIA base_url (works if only langchain-openai installed).
    if provider == "nvidia":
        model   = os.getenv("NVIDIA_MODEL", "meta/llama-3.3-70b-instruct")
        api_key = os.getenv("NVIDIA_API_KEY")
        log.info("LLM provider=nvidia  model=%s  temperature=%.1f", model, temperature)
        try:
            from langchain_nvidia_ai_endpoints import ChatNVIDIA
            log.info("Using ChatNVIDIA (langchain-nvidia-ai-endpoints)")
            return ChatNVIDIA(
                model=model,
                temperature=temperature,
                nvidia_api_key=api_key,
                max_tokens=1024,
            )
        except ImportError:
            from langchain_openai import ChatOpenAI
            log.warning(
                "langchain-nvidia-ai-endpoints not installed — "
                "falling back to ChatOpenAI + NVIDIA base_url. "
                "Run: pip install langchain-nvidia-ai-endpoints"
            )
            return ChatOpenAI(
                model=model,
                temperature=temperature,
                api_key=api_key,
                base_url="https://integrate.api.nvidia.com/v1",
                max_tokens=1024,
            )

    # ── OpenAI ────────────────────────────────────────────────────────────────
    if provider == "openai":
        from langchain_openai import ChatOpenAI
        model = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
        log.info("LLM provider=openai  model=%s  temperature=%.1f", model, temperature)
        return ChatOpenAI(
            model=model,
            temperature=temperature,
            api_key=os.getenv("OPENAI_API_KEY"),
        )

    # ── Google Gemini ─────────────────────────────────────────────────────────
    if provider == "gemini":
        from langchain_google_genai import ChatGoogleGenerativeAI
        model = os.getenv("GEMINI_MODEL", "gemini-2.0-flash")
        log.info("LLM provider=gemini  model=%s  temperature=%.1f", model, temperature)
        return ChatGoogleGenerativeAI(
            model=model,
            temperature=temperature,
            google_api_key=os.getenv("GOOGLE_API_KEY"),
        )

    raise ValueError(
        f"Unknown LLM_PROVIDER='{provider}'. "
        "Valid values: nvidia (default) | openai | gemini"
    )


def _parse_llm_json(raw: str) -> dict:
    """
    Safely parse JSON from LLM output.
    Strips markdown code fences (```json ... ```) if present.
    """
    cleaned = raw.strip()
    # Remove leading ```json or ``` fence
    if cleaned.startswith("```"):
        cleaned = cleaned.split("\n", 1)[-1]  # drop first line
    # Remove trailing ``` fence
    if cleaned.endswith("```"):
        cleaned = cleaned.rsplit("```", 1)[0]
    return json.loads(cleaned.strip())


# ══════════════════════════════════════════════════════════════════════════════
# SECTION 5  ·  L3 AGENT SYSTEM PROMPTS
# ══════════════════════════════════════════════════════════════════════════════
#
# Each L3 agent has a tightly scoped system prompt that instructs the LLM to
# behave as that specialist. Output is always strict JSON — no fences, no prose.
# ══════════════════════════════════════════════════════════════════════════════

L3_SYSTEM_PROMPTS: dict[L3Agent, str] = {

    L3Agent.ACTION_ITEM_EXTRACTION: """
You are the action_item_extraction agent for the NION AI Program Manager.
Extract every concrete action item from the provided text.
Return ONLY a valid JSON object — no markdown, no explanation:
{
  "action_items": [
    {
      "id": "AI-001",
      "description": "short task description",
      "owner": "person name or team",
      "due_date": "YYYY-MM-DD or relative e.g. 'Monday'",
      "priority": "HIGH | MEDIUM | LOW",
      "status": "OPEN"
    }
  ],
  "total_count": 0
}
""",

    L3Agent.RISK_TRACKING: """
You are the risk_tracking agent for the NION AI Program Manager.
Identify all risks, blockers, and threats in the provided text.
Return ONLY valid JSON:
{
  "risks": [
    {
      "id": "RISK-001",
      "description": "risk description",
      "severity": "HIGH | MEDIUM | LOW",
      "likelihood": "HIGH | MEDIUM | LOW",
      "impact": "brief impact statement",
      "mitigation": "suggested mitigation",
      "owner": "who should address this"
    }
  ],
  "overall_risk_level": "HIGH | MEDIUM | LOW"
}
""",

    L3Agent.DECISION_TRACKING: """
You are the decision_tracking agent for the NION AI Program Manager.
Extract all explicit or implied decisions from the provided text.
Return ONLY valid JSON:
{
  "decisions": [
    {
      "id": "DEC-001",
      "decision": "what was decided",
      "rationale": "why this decision was made",
      "made_by": "person or team",
      "date": "date mentioned or 'unspecified'",
      "impact": "brief impact statement",
      "next_steps": "follow-on actions"
    }
  ],
  "pending_decisions": ["list of decisions that still need to be made"]
}
""",

    L3Agent.PROGRESS_MONITORING: """
You are the progress_monitoring agent for the NION AI Program Manager.
Summarise the current project or task progress based on the provided text.
Return ONLY valid JSON:
{
  "progress_summary": "concise progress description",
  "completion_percentage": 0,
  "velocity_indicator": "AHEAD | ON_TRACK | SLIGHTLY_BEHIND | AT_RISK",
  "completed_items": ["list of completed items mentioned"],
  "in_progress_items": ["list of items in progress"],
  "blockers": ["list of current blockers"],
  "on_track": true
}
""",

    L3Agent.DEADLINE_MANAGEMENT: """
You are the deadline_management agent for the NION AI Program Manager.
Identify all deadlines, dates, and timeline commitments in the text.
Return ONLY valid JSON:
{
  "deadlines": [
    {
      "task": "task or deliverable name",
      "due_date": "date",
      "status": "ON_TRACK | AT_RISK | OVERDUE | POSTPONED",
      "days_remaining": null,
      "owner": "person or team",
      "notes": "any relevant context"
    }
  ],
  "critical_path_risk": true
}
""",

    L3Agent.MESSAGE_DELIVERY: """
You are the message_delivery agent for the NION AI Program Manager.
Draft a clear, actionable delivery message based on the provided context.
Return ONLY valid JSON:
{
  "recipient": "target person or group",
  "channel": "email | slack | teams | sms",
  "subject": "message subject",
  "body": "full message body",
  "urgency": "HIGH | MEDIUM | LOW",
  "requires_response": true,
  "response_deadline": "timeframe or null"
}
""",

    L3Agent.STAKEHOLDER_NOTIFICATION: """
You are the stakeholder_notification agent for the NION AI Program Manager.
Compose a professional stakeholder notification based on the context.
Return ONLY valid JSON:
{
  "stakeholders": ["list of stakeholder names or roles"],
  "notification_type": "status_update | risk_alert | decision_needed | incident",
  "subject": "notification subject",
  "message": "full notification message",
  "requires_ack": false,
  "escalation_path": "fallback contact if no response"
}
""",

    L3Agent.MEETING_SUMMARY: """
You are the meeting_summary agent for the NION AI Program Manager.
Generate structured, professional meeting minutes from the transcript.
Return ONLY valid JSON:
{
  "meeting_title": "meeting name",
  "date": "meeting date",
  "attendees": ["list of names"],
  "key_topics": ["list of topics discussed"],
  "decisions": ["list of decisions made"],
  "action_items": [{"task": "...", "owner": "...", "due": "..."}],
  "risks_raised": ["list of risks mentioned"],
  "next_meeting": "suggested date or null",
  "summary": "2-3 sentence executive summary"
}
""",

    L3Agent.ESCALATION_HANDLER: """
You are the escalation_handler agent for the NION AI Program Manager.
Define the escalation path and response plan for the given situation.
Return ONLY valid JSON:
{
  "escalation_level": "P1 | P2 | P3 | P4",
  "escalate_to": "person or role to escalate to",
  "reason": "why escalation is needed",
  "suggested_response_time": "e.g. 15 minutes | 1 hour | 4 hours",
  "notification_channels": ["list of channels"],
  "immediate_actions": ["list of actions to take right now"],
  "war_room_needed": false,
  "customer_impact": "HIGH | MEDIUM | LOW | NONE"
}
""",

    L3Agent.QNA: """
You are the QnA agent for the NION AI Program Manager.
Answer the question in the input using the provided project context.
Return ONLY valid JSON:
{
  "question": "the question being answered",
  "answer": "direct, concise answer",
  "confidence": 0.85,
  "evidence": ["supporting facts from context"],
  "caveats": ["any important limitations or assumptions"],
  "follow_up_suggested": false
}
""",

    L3Agent.RETROSPECTIVE_ANALYSIS: """
You are the retrospective_analysis agent for the NION AI Program Manager.
Analyse past performance and extract retro insights from the provided text.
Return ONLY valid JSON:
{
  "went_well": ["list of positives"],
  "improvements_needed": ["list of areas to improve"],
  "root_causes": ["identified root causes for problems"],
  "sprint_velocity": "qualitative assessment",
  "team_morale_signal": "POSITIVE | NEUTRAL | CONCERNING",
  "key_learning": "single most important learning"
}
""",

    L3Agent.PATTERN_RECOGNITION: """
You are the pattern_recognition agent for the NION AI Program Manager.
Identify recurring themes, patterns, or systemic issues in the provided text.
Return ONLY valid JSON:
{
  "patterns": [
    {
      "pattern": "description of the pattern",
      "frequency": "e.g. repeated across 3 sprints",
      "impact": "HIGH | MEDIUM | LOW",
      "category": "process | technical | people | external"
    }
  ],
  "trend": "IMPROVING | STABLE | DEGRADING",
  "systemic_issues": ["issues that appear structural"],
  "anomalies": ["one-off unexpected events"]
}
""",

    L3Agent.KNOWLEDGE_BASE_UPDATE: """
You are the knowledge_base_update agent for the NION AI Program Manager.
Identify what should be added, updated, or deprecated in the project knowledge base.
Return ONLY valid JSON:
{
  "updates": [
    {
      "entry_title": "KB article or entry title",
      "action": "CREATE | UPDATE | DEPRECATE",
      "reason": "why this update is needed",
      "proposed_content": "brief summary of the content",
      "tags": ["relevant tags"]
    }
  ],
  "kb_health_note": "brief note on KB currency"
}
""",

    L3Agent.IMPROVEMENT_SUGGESTIONS: """
You are the improvement_suggestions agent for the NION AI Program Manager.
Recommend actionable process or technical improvements based on the context.
Return ONLY valid JSON:
{
  "suggestions": [
    {
      "area": "process | technical | communication | tooling",
      "suggestion": "specific, actionable recommendation",
      "expected_impact": "what benefit this will deliver",
      "effort": "LOW | MEDIUM | HIGH",
      "priority": "HIGH | MEDIUM | LOW"
    }
  ],
  "quick_wins": ["suggestions achievable in < 1 week"]
}
""",
}


# ══════════════════════════════════════════════════════════════════════════════
# SECTION 6  ·  CROSS-CUTTING AGENTS
# ══════════════════════════════════════════════════════════════════════════════

class CrossCuttingCoordinator:
    """
    Manages the two cross-cutting agents: knowledge_retrieval and evaluation.

    These agents are NOT owned by any L2 domain — they are shared services
    accessible to both L1 (orchestrator) and all L2 coordinators.

    Visibility: both L1 and L2 can invoke these agents.
    """

    _log = get_logger("cross_cutting")

    def __init__(self, llm) -> None:
        self.llm = llm

    # ── knowledge_retrieval ───────────────────────────────────────────────────
    def run_knowledge_retrieval(self, message: str) -> CrossCuttingResult:
        """Retrieve relevant project context before L2 execution."""
        self._log.info("knowledge_retrieval · fetching project context for message")

        prompt = ChatPromptTemplate.from_messages([
            ("system", """You are the knowledge_retrieval agent for Project Phoenix.
You have access to project history, sprint backlogs, team roster, and past decisions.
Simulate a realistic knowledge retrieval response for this project management scenario.
Return ONLY valid JSON — no markdown fences:
{{
  "project_name": "Project Phoenix",
  "context_retrieved": ["relevant fact 1", "relevant fact 2", "relevant fact 3"],
  "relevant_sprint": "Sprint 14",
  "team_members": ["list of team members"],
  "recent_decisions": ["last key decision"],
  "relevance_score": 0.87
}}"""),
            ("human", "Retrieve context relevant to: {message}"),
        ])
        chain = prompt | self.llm | StrOutputParser()

        try:
            raw = chain.invoke({"message": message})
            output = _parse_llm_json(raw)
            self._log.info("knowledge_retrieval · SUCCESS (relevance=%.2f)",
                           output.get("relevance_score", 0.0))
            return CrossCuttingResult(agent="knowledge_retrieval", status="SUCCESS", output=output)
        except Exception as exc:
            self._log.warning("knowledge_retrieval · fallback triggered. Error: %s", exc)
            return CrossCuttingResult(
                agent="knowledge_retrieval",
                status="PARTIAL",
                output={
                    "project_name": "Project Phoenix",
                    "context_retrieved": [
                        "Sprint 14 in progress — 6 engineers, 2-week cadence",
                        "API integration task is 70% complete (owner: James)",
                        "Q3 hard deadline: July 31st",
                        "Last decision: Postponed v2 API launch to July 15th",
                    ],
                    "relevant_sprint": "Sprint 14",
                    "team_members": ["Sarah (PM)", "James (Lead Dev)", "Priya (QA)", "Tom (DevOps)"],
                    "recent_decisions": ["Postpone v2 API launch by 2 weeks"],
                    "relevance_score": 0.80,
                },
            )

    # ── evaluation ────────────────────────────────────────────────────────────
    def run_evaluation(self, message: str, l2_summaries: list[str]) -> CrossCuttingResult:
        """Evaluate tone, accuracy, and completeness of L2 outputs."""
        self._log.info("evaluation · quality-checking %d domain outputs", len(l2_summaries))

        prompt = ChatPromptTemplate.from_messages([
            ("system", """You are the evaluation agent for the NION AI Program Manager.
Assess the quality, tone, and completeness of the AI-generated project management outputs below.
Return ONLY valid JSON — no markdown fences:
{{
  "tone_ok": true,
  "tone_notes": "brief tone assessment",
  "accuracy_score": 8,
  "completeness_score": 7,
  "flags": ["any issues found"],
  "overall_quality": "EXCELLENT | GOOD | ACCEPTABLE | NEEDS_REVIEW",
  "recommendation": "brief recommendation for improvement if any"
}}"""),
            ("human",
             "Original message:\n{message}\n\nGenerated domain summaries:\n{summaries}"),
        ])
        chain = prompt | self.llm | StrOutputParser()

        try:
            raw = chain.invoke({
                "message": message,
                "summaries": "\n\n".join(l2_summaries) or "(no summaries yet)",
            })
            output = _parse_llm_json(raw)
            self._log.info("evaluation · SUCCESS (quality=%s)", output.get("overall_quality", "N/A"))
            return CrossCuttingResult(agent="evaluation", status="SUCCESS", output=output)
        except Exception as exc:
            self._log.warning("evaluation · fallback triggered. Error: %s", exc)
            return CrossCuttingResult(
                agent="evaluation",
                status="PARTIAL",
                output={
                    "tone_ok": True,
                    "tone_notes": "Professional and factual",
                    "accuracy_score": 8,
                    "completeness_score": 7,
                    "flags": [],
                    "overall_quality": "GOOD",
                    "recommendation": "None — outputs meet quality standards",
                },
            )


# ══════════════════════════════════════════════════════════════════════════════
# SECTION 7  ·  L3 AGENT RUNNER
# ══════════════════════════════════════════════════════════════════════════════

class L3AgentRunner:
    """
    Generic runner for all L3 specialist agents.

    Each agent call:
      1. Builds a prompt from the agent-specific system prompt + the message
      2. Invokes the LLM
      3. Parses the JSON output
      4. Returns a structured L3AgentResult

    The runner is unaware of L2 domain membership — it simply executes
    whatever agent it is asked to run. Domain scoping is enforced by L2.
    """

    _log = get_logger("l3.runner")

    def __init__(self, llm) -> None:
        self.llm = llm

    def run(
        self,
        agent: L3Agent,
        message: str,
        context: str,
        task_id: str,
    ) -> L3AgentResult:
        """Execute a single L3 agent and return its structured result."""
        start_ms = int(time.time() * 1000)
        self._log.info("[%s] ▶  %s  starting", task_id, agent.value)

        system_prompt = L3_SYSTEM_PROMPTS.get(
            agent,
            "Process the input and return a valid JSON object with your findings.",
        )

        # ── CRITICAL: Escape all braces in the system prompt ─────────────────
        # LangChain's ChatPromptTemplate treats any {word} as a template variable.
        # Our JSON schema examples use { } heavily — they must be escaped to {{ }}
        # so LangChain renders them as literal braces, not "missing variables".
        # Only {context} and {message} in the HUMAN turn stay as real variables.
        system_prompt_escaped = (
            system_prompt.strip()
            .replace("{", "{{")
            .replace("}", "}}")
        )

        prompt = ChatPromptTemplate.from_messages([
            ("system", system_prompt_escaped + "\n\nReturn ONLY valid JSON — no markdown fences."),
            ("human",
             "Project context:\n{context}\n\n---\nInput message:\n{message}"),
        ])
        chain = prompt | self.llm | StrOutputParser()

        try:
            raw = chain.invoke({"message": message, "context": context})
            output = _parse_llm_json(raw)
            status = "SUCCESS"
            self._log.info("[%s] ✓  %s  SUCCESS", task_id, agent.value)
        except Exception as exc:
            self._log.warning("[%s] ~  %s  PARTIAL/FAILED: %s", task_id, agent.value, exc)
            output = {
                "_note": "LLM parse failed — partial fallback response",
                "_agent": agent.value,
                "_error": str(exc)[:120],
            }
            status = "PARTIAL"

        exec_time_ms = int(time.time() * 1000) - start_ms
        return L3AgentResult(
            agent=agent.value,
            task_id=task_id,
            status=status,
            output=output,
            exec_time_ms=exec_time_ms,
        )


# ══════════════════════════════════════════════════════════════════════════════
# SECTION 8  ·  L2 COORDINATORS
# ══════════════════════════════════════════════════════════════════════════════

class L2Coordinator:
    """
    L2 Domain Coordinator.

    Responsibilities:
      • Receive a PlannedTask from L1 (task_id, rationale, agents_hint, priority)
      • Select the most relevant L3 agents within its VISIBILITY SCOPE
      • Run those L3 agents via L3AgentRunner
      • Aggregate results and produce a domain summary

    Visibility enforcement (non-negotiable):
      • self.visible_agents = DOMAIN_AGENTS[domain] only
      • Cross-cutting agents are also available
      • Agents from OTHER L2 domains are NEVER in scope — enforced by
        the visibility check inside _select_agents()
    """

    def __init__(self, domain: L2Domain, llm) -> None:
        self.domain = domain
        self.llm    = llm
        self._log   = get_logger(f"l2.{domain.value.lower()}")

        # ── VISIBILITY SCOPE (enforced here) ─────────────────────────────────
        self._own_agents: list[L3Agent]     = DOMAIN_AGENTS[domain]
        self._visible_agents: list[L3Agent] = self._own_agents + CROSS_CUTTING_AGENTS

        self._l3_runner = L3AgentRunner(llm)

    # ── Agent Selection ───────────────────────────────────────────────────────
    def _select_agents(self, task: PlannedTask) -> list[L3Agent]:
        """
        Use the LLM to choose the most relevant L3 agents from this domain's
        own agent roster. Cross-cutting agents (knowledge_retrieval, evaluation)
        are handled separately by the orchestrator — not selected here.

        VISIBILITY GUARD: every selected name is validated against
        self._own_agents before being used.
        """
        own_names = [a.value for a in self._own_agents]

        prompt = ChatPromptTemplate.from_messages([
            ("system", f"""You are the {self.domain.value} coordinator.
Select the most relevant agents for the given task from your agent roster.
Available agents (you may ONLY choose from this list): {own_names}
Return ONLY valid JSON: {{"selected_agents": ["agent_name_1", "agent_name_2"]}}
Choose 1-3 agents. Prefer quality over quantity."""),
            ("human", "Task rationale: {rationale}\nSuggested hints: {hints}"),
        ])
        chain = prompt | self.llm | StrOutputParser()

        selected: list[L3Agent] = []
        try:
            raw = chain.invoke({
                "rationale": task.rationale,
                "hints": ", ".join(task.agents_hint),
            })
            data = _parse_llm_json(raw)
            for name in data.get("selected_agents", []):
                try:
                    agent = L3Agent(name)
                    # ── VISIBILITY GUARD ─────────────────────────────────────
                    if agent in self._own_agents:
                        selected.append(agent)
                    else:
                        self._log.warning(
                            "VISIBILITY VIOLATION blocked: %s is not in domain %s",
                            name, self.domain.value,
                        )
                except ValueError:
                    self._log.warning("Unknown agent name skipped: %s", name)

        except Exception as exc:
            self._log.warning("Agent selection LLM call failed (%s) — using hint fallback", exc)

        if not selected:
            # Fallback: match hints against own agents, else take first two
            for hint in task.agents_hint:
                try:
                    agent = L3Agent(hint)
                    if agent in self._own_agents:
                        selected.append(agent)
                except ValueError:
                    pass
            if not selected:
                selected = self._own_agents[:2]

        self._log.info("[%s] Selected agents: %s", task.task_id,
                       [a.value for a in selected])
        return selected

    # ── Main Execution ────────────────────────────────────────────────────────
    def execute(
        self,
        task: PlannedTask,
        message: str,
        context: str,
    ) -> L2DomainResult:
        """Coordinate L3 execution for the given task."""
        self._log.info("[%s] %s coordinator starting", task.task_id, self.domain.value)

        agents_to_run = self._select_agents(task)
        results: list[L3AgentResult] = []

        for agent in agents_to_run:
            result = self._l3_runner.run(agent, message, context, task.task_id)
            results.append(result)

        summary = self._build_summary(task, results)
        self._log.info("[%s] %s coordinator complete (%d/%d agents succeeded)",
                       task.task_id, self.domain.value,
                       sum(1 for r in results if r.status == "SUCCESS"),
                       len(results))

        return L2DomainResult(
            domain=self.domain.value,
            task_id=task.task_id,
            agents_invoked=[a.value for a in agents_to_run],
            results=results,
            summary=summary,
        )

    def _build_summary(self, task: PlannedTask, results: list[L3AgentResult]) -> str:
        success = sum(1 for r in results if r.status == "SUCCESS")
        return (
            f"{self.domain.value} | {task.task_id} | "
            f"{success}/{len(results)} agents succeeded | "
            f"Priority: {task.priority}"
        )


# ══════════════════════════════════════════════════════════════════════════════
# SECTION 9  ·  L1 ORCHESTRATOR  (ROUTER PATTERN)
# ══════════════════════════════════════════════════════════════════════════════

class L1Orchestrator:
    """
    L1 Orchestrator — the top-level intelligence layer.

    Responsibilities:
      • Analyse the incoming message for intent and gaps (LangChain Router)
      • Generate a structured routing plan (L1Plan) with task IDs
      • Dispatch tasks to the correct L2 coordinators
      • Invoke cross-cutting agents at the appropriate phases
      • Aggregate all results into a final OrchestrationResult

    Visibility (non-negotiable):
      • L1 sees ONLY L2 domain names — it has NO reference to L3Agent enum values
      • It passes "agents_hint" strings as advisory text, not direct invocations
      • L2 coordinators are the ONLY entities L1 dispatches to directly
    """

    _log = get_logger("l1.orchestrator")

    def __init__(self, llm) -> None:
        self.llm = llm
        self._cross_cutting = CrossCuttingCoordinator(llm)

        # ── L2 coordinators — L1 only knows about domain names, not L3 agents
        self._l2_coordinators: dict[L2Domain, L2Coordinator] = {
            domain: L2Coordinator(domain, llm) for domain in L2Domain
        }
        self._build_router_chain()

    # ── LangChain Router Chain ────────────────────────────────────────────────
    def _build_router_chain(self) -> None:
        """
        Build the LangChain LCEL router chain used by L1.

        Chain  :  ChatPromptTemplate.from_template  →  LLM  →  StrOutputParser

        ── Why from_template (not from_messages with an f-string) ─────────────
        The previous implementation used an f-string to inject json.dumps() into
        the system message. json.dumps produces bare { } characters which
        LangChain's template parser then mis-reads as missing input variables,
        causing PromptValidationError: missing variables ['intent', 'TRACKING_EXECUTION'].

        from_template receives a SINGLE plain string (not an f-string).
        Every literal brace in the JSON schema is escaped as {{ or }}.
        The ONLY single-brace placeholders are {message} and {context} —
        the two actual runtime variables this chain receives.

        ── L1 Visibility enforcement ───────────────────────────────────────────
        The domain descriptions are hardcoded as plain text — no json.dumps,
        no f-string interpolation. L3 agent names NEVER appear in this prompt.
        L1 only learns about the three L2 domain names and their purposes.
        """
        # ── IMPORTANT ─────────────────────────────────────────────────────────
        # This is NOT an f-string. Do NOT add the f prefix.
        # All {{ }} are literal braces for LangChain's template renderer.
        # Only {message} and {context} are real input variables.
        # ── IMPORTANT ─────────────────────────────────────────────────────────
        self._router_chain = (
            ChatPromptTemplate.from_template(
"""You are NION, an enterprise L1 AI Program Manager Orchestrator.
Your ONLY job is to analyse an incoming project communication and produce \
a structured JSON routing plan for the correct L2 domain(s).

=======================================================================
SECTION 1 — PROJECT CONTEXT  (retrieved before planning)
=======================================================================
{context}

=======================================================================
SECTION 2 — L2 DOMAINS YOU MAY ROUTE TO  (your complete visibility)
=======================================================================
You must choose from EXACTLY these three L2 domain names and nothing else.
You have NO knowledge of the specific agents inside each domain.

  TRACKING_EXECUTION
    Purpose: Track and extract structured work items from communications.
    Covers : action items with owners/dates, risk identification, decision
             logging, progress status, and deadline management.

  COMMUNICATION_COLLABORATION
    Purpose: Handle all messaging, notifications, and meeting artefacts.
    Covers : drafting messages, stakeholder notifications, meeting minutes,
             escalation routing, and answering project questions (Q&A).

  LEARNING_IMPROVEMENT
    Purpose: Extract insights and drive continuous improvement.
    Covers : retrospective analysis, recurring pattern detection, knowledge
             base updates, and process improvement recommendations.

Cross-cutting services available for any task:
  knowledge_retrieval — retrieves project history and context (already run above)
  evaluation          — checks quality, tone, and accuracy of outputs

=======================================================================
SECTION 3 — STRICT VISIBILITY RULES  (non-negotiable)
=======================================================================
RULE 1: You MUST NOT name any specific internal agents or tools inside a domain.
        The agents within each domain are invisible to you — refer only to
        domain-level capabilities (e.g. "extract action items") not agent names.
RULE 2: The "agents_hint" field must only contain plain English capability
        descriptions, never agent code names.
RULE 3: Route to only the L2 domains that are genuinely needed.
        Do not add a domain just to be thorough.
RULE 4: For escalations always include COMMUNICATION_COLLABORATION as HIGH.
RULE 5: For ambiguous messages still produce a best-effort plan and list gaps.

=======================================================================
SECTION 4 — REQUIRED OUTPUT FORMAT
=======================================================================
Return ONLY a valid JSON object. No markdown fences. No explanation text.
Use EXACTLY this structure — fill in every field:

{{
  "intent": "one sentence describing what the sender needs",
  "message_type": "status_query | feasibility | decision_request | meeting_transcript | escalation | ambiguous",
  "gaps": ["list any missing context or ambiguities; use empty list [] if none"],
  "tasks": [
    {{
      "task_id": "TASK-001",
      "domain": "TRACKING_EXECUTION",
      "rationale": "explain in one sentence why this domain is needed",
      "agents_hint": ["plain English description of the capability needed, e.g. extract action items with owners"],
      "priority": "HIGH | MEDIUM | LOW"
    }},
    {{
      "task_id": "TASK-002",
      "domain": "COMMUNICATION_COLLABORATION",
      "rationale": "explain why this domain is needed",
      "agents_hint": ["plain English capability description"],
      "priority": "HIGH | MEDIUM | LOW"
    }}
  ],
  "cross_cutting_needed": ["knowledge_retrieval", "evaluation"]
}}

Notes:
  • Include only the tasks whose domains are genuinely required (1–3 tasks).
  • task_id values must be TASK-001, TASK-002, TASK-003 in order.
  • "domain" must be one of the three exact strings above — nothing else.

=======================================================================
SECTION 5 — MESSAGE TO ANALYSE
=======================================================================
{message}"""
            )
            | self.llm
            | StrOutputParser()
        )

    # ── Planning ──────────────────────────────────────────────────────────────
    def plan(self, message: str, context: str = "") -> L1Plan:
        """
        Invoke the router chain to produce a structured L1Plan.

        Args:
            message : The raw incoming project communication.
            context : Pre-fetched knowledge_retrieval output (JSON string).
                      Injected into the prompt so L1 reasons with project context
                      before deciding which L2 domains to activate.
        """
        self._log.info("L1 · planning (message_len=%d chars  context_len=%d chars)",
                       len(message), len(context))
        try:
            raw  = self._router_chain.invoke({"message": message, "context": context})
            data = _parse_llm_json(raw)
            plan = L1Plan(**data)
            self._log.info(
                "L1 · plan ready | type=%s | tasks=%d | domains=%s",
                plan.message_type,
                len(plan.tasks),
                [t.domain for t in plan.tasks],
            )
            return plan
        except Exception as exc:
            self._log.error("L1 · planning FAILED (%s) — using fallback plan", exc)
            return self._fallback_plan(message)

    def _fallback_plan(self, message: str) -> L1Plan:
        """Emergency fallback plan if the LLM router call fails."""
        return L1Plan(
            intent="Process incoming project communication (fallback mode)",
            message_type="ambiguous",
            gaps=["L1 planning LLM call failed — defaulting to tracking domain"],
            tasks=[
                PlannedTask(
                    task_id="TASK-001",
                    domain="TRACKING_EXECUTION",
                    rationale="Default domain for unclassified messages",
                    agents_hint=["action_item_extraction"],
                    priority="MEDIUM",
                )
            ],
            cross_cutting_needed=["knowledge_retrieval", "evaluation"],
        )

    # ── Main Orchestration ────────────────────────────────────────────────────
    def orchestrate(self, input_json: dict) -> OrchestrationResult:
        """
        Full 4-phase orchestration flow.

        Phase 1a │ Cross-cutting: knowledge_retrieval  ← runs FIRST so L1 has context
        Phase 1b │ L1 Router Planning                  ← receives context, produces L1Plan
        Phase 2  │ L2 Domain Execution                 ← coordinators invoke L3 agents
        Phase 3  │ Cross-cutting: evaluation            ← quality-checks all outputs

        Why knowledge_retrieval is in Phase 1a (not Phase 2):
          L1 reasons better about WHICH domains to activate when it already knows
          the project context (current sprint, team members, recent decisions).
          Passing it as {context} into the planning prompt means the LLM routes
          with facts instead of guessing, producing more precise L1Plans.
        """
        message_id = f"NION-{datetime.now().strftime('%Y%m%d-%H%M%S')}"
        message    = input_json.get("message", json.dumps(input_json))

        self._log.info("━" * 60)
        self._log.info("ORCHESTRATION START  id=%s", message_id)
        self._log.info("━" * 60)

        # ── Phase 1a: knowledge_retrieval runs BEFORE planning ────────────────
        # This is the key integration: context feeds directly into the L1 prompt.
        self._log.info("── PHASE 1a · Cross-Cutting: knowledge_retrieval (pre-planning)")
        context_result = self._cross_cutting.run_knowledge_retrieval(message)
        context_str    = json.dumps(context_result.output, indent=2)

        # ── Phase 1b: L1 Router Planning (with context) ───────────────────────
        self._log.info("── PHASE 1b · L1 Router Planning (context injected)")
        plan = self.plan(message, context=context_str)

        # ── Phase 2: L2 Domain Execution ──────────────────────────────────────
        self._log.info("── PHASE 2 · L2 Domain Execution (%d tasks)", len(plan.tasks))
        l2_results: list[L2DomainResult] = []

        for task in plan.tasks:
            try:
                domain      = L2Domain(task.domain)          # validates domain name
                coordinator = self._l2_coordinators[domain]  # L1 dispatches to L2 only
                result      = coordinator.execute(task, message, context_str)
                l2_results.append(result)
            except (ValueError, KeyError) as exc:
                self._log.error(
                    "L2 dispatch FAILED for domain=%s  task=%s  error=%s",
                    task.domain, task.task_id, exc,
                )

        # ── Phase 3: Post-execution Evaluation ───────────────────────────────
        self._log.info("── PHASE 3 · Cross-Cutting: evaluation")
        summaries  = [r.summary for r in l2_results]
        eval_result = self._cross_cutting.run_evaluation(message, summaries)

        # Build final cross-cutting results list
        cross_cutting_results: list[CrossCuttingResult] = [context_result]
        if "evaluation" in (plan.cross_cutting_needed or []):
            cross_cutting_results.append(eval_result)

        self._log.info("ORCHESTRATION COMPLETE  id=%s  l2_domains=%d",
                       message_id, len(l2_results))

        return OrchestrationResult(
            message_id=message_id,
            timestamp=datetime.now().isoformat(),
            l1_plan=plan,
            l2_results=l2_results,
            cross_cutting_results=cross_cutting_results,
        )


# ══════════════════════════════════════════════════════════════════════════════
# SECTION 10  ·  OUTPUT FORMATTER  (NION ORCHESTRATION MAP)
# ══════════════════════════════════════════════════════════════════════════════

_MAP_WIDTH = 72  # character width for the map output


def _hr(char: str = "─") -> str:
    return char * _MAP_WIDTH


def _section(icon: str, title: str) -> str:
    return f"\n{_hr()}\n  {icon}  {title}\n{_hr()}"


def _truncate(val: Any, max_len: int = 75) -> str:
    s = str(val)
    return s[:max_len] + "…" if len(s) > max_len else s


def format_orchestration_map(result: OrchestrationResult) -> str:
    """
    Renders the official NION ORCHESTRATION MAP in the required format.

    Sections:
      Header → L1 PLAN → L2/L3 EXECUTION → CROSS-CUTTING → Footer
    """
    lines: list[str] = []

    # ── Header ────────────────────────────────────────────────────────────────
    lines += [
        "",
        f"╔{'═' * (_MAP_WIDTH - 2)}╗",
        f"║{'NION ORCHESTRATION MAP  ·  aiNions Enterprise AI Platform':^{_MAP_WIDTH - 2}}║",
        f"╚{'═' * (_MAP_WIDTH - 2)}╝",
        f"  Message ID  :  {result.message_id}",
        f"  Timestamp   :  {result.timestamp}",
    ]

    # ── L1 PLAN ───────────────────────────────────────────────────────────────
    lines.append(_section("📋", "L1 PLAN  (Orchestrator Layer)"))
    plan = result.l1_plan
    lines += [
        f"  Intent        :  {plan.intent}",
        f"  Message Type  :  {plan.message_type.upper()}",
    ]

    if plan.gaps:
        lines.append("  Gaps / Missing Context:")
        for gap in plan.gaps:
            lines.append(f"    ⚠  {gap}")
    else:
        lines.append("  Gaps / Missing Context  :  None identified ✓")

    lines.append("\n  Routing Plan:")
    for task in plan.tasks:
        lines += [
            f"\n  [{task.task_id}]  ──▶  {task.domain}",
            f"    │  Rationale  :  {_truncate(task.rationale, 65)}",
            f"    │  Priority   :  {task.priority}",
            f"    └  Hints      :  {', '.join(task.agents_hint)}",
        ]

    cc_needed = ", ".join(plan.cross_cutting_needed) if plan.cross_cutting_needed else "None"
    lines.append(f"\n  Cross-Cutting Requested  :  {cc_needed}")

    # ── L2 / L3 EXECUTION ─────────────────────────────────────────────────────
    lines.append(_section("⚙️ ", "L2 / L3 EXECUTION  (Coordinator → Agent Layer)"))

    for domain_result in result.l2_results:
        lines += [
            "",
            f"  ┌── [{domain_result.task_id}]  {domain_result.domain}",
            f"  │   Summary  :  {domain_result.summary}",
            f"  │   Agents   :  {', '.join(domain_result.agents_invoked)}",
            "  │",
        ]
        for agent_result in domain_result.results:
            icon  = "✓" if agent_result.status == "SUCCESS" else "⚠"
            lines.append(
                f"  ├──▶ [{icon}] {agent_result.agent:<38}  "
                f"({agent_result.exec_time_ms}ms  {agent_result.status})"
            )
            if isinstance(agent_result.output, dict):
                for key, val in list(agent_result.output.items())[:3]:
                    if not key.startswith("_"):  # skip internal _note/_error fields
                        lines.append(f"  │         {key}  :  {_truncate(val)}")
            lines.append("  │")
        lines.append(f"  └{'─' * (_MAP_WIDTH - 4)}")

    # ── CROSS-CUTTING ─────────────────────────────────────────────────────────
    lines.append(_section("🔗", "CROSS-CUTTING AGENTS  (Shared Services)"))

    for cc in result.cross_cutting_results:
        icon = "✓" if cc.status == "SUCCESS" else "⚠"
        lines.append(f"\n  [{icon}]  {cc.agent}  ({cc.status})")
        if isinstance(cc.output, dict):
            for key, val in list(cc.output.items())[:4]:
                lines.append(f"        {key}  :  {_truncate(val)}")

    # ── Footer ────────────────────────────────────────────────────────────────
    lines += [
        "",
        _hr("═"),
        "  ✅  NION ORCHESTRATION MAP COMPLETE",
        _hr("═"),
        "",
    ]

    return "\n".join(lines)


# ══════════════════════════════════════════════════════════════════════════════
# SECTION 11  ·  TEST CASES
# ══════════════════════════════════════════════════════════════════════════════

TEST_CASES: list[dict[str, str]] = [
    {
        "id":      "TC-01",
        "name":    "Simple Status Question",
        "message": (
            "Hi Nion, can you give me the current status of the API integration task? "
            "I need to know if we're on track for the Q3 deadline. "
            "Also, has James submitted his progress report yet?"
        ),
    },
    {
        "id":      "TC-02",
        "name":    "Feasibility Question",
        "message": (
            "Is it feasible to deliver the mobile app feature (push notifications + offline mode) "
            "by end of Q2? We currently have 4 engineers and 6 weeks remaining. "
            "Our last two sprints had an average velocity of 32 story points."
        ),
    },
    {
        "id":      "TC-03",
        "name":    "Decision Request",
        "message": (
            "The team needs to make a final decision on the database technology for the new "
            "user-profile microservice. The two options are PostgreSQL vs MongoDB. "
            "The decision must be finalised by Friday EOD and communicated to the "
            "infrastructure team so they can provision the environment next week."
        ),
    },
    {
        "id":      "TC-04",
        "name":    "Meeting Transcript",
        "message": textwrap.dedent("""
            [Meeting Transcript — Project Phoenix Sprint 14 Review, 2024-06-15]

            Attendees: Sarah (PM), James (Lead Dev), Priya (QA), Tom (DevOps), Aisha (Scrum Master)

            Sarah: Let's start with sprint highlights. James, how did the auth module go?
            James: Auth module is done and merged into main. However, we discovered a security
                   vulnerability in our JWT token refresh logic. I'll need an extra day to patch it —
                   estimate Monday delivery.
            Sarah: That's a risk. Let's flag it. Priya, test coverage status?
            Priya: We're at 74% overall coverage. We need 80% before the release gate. I'll need
                   James to write unit tests for the payment module — targeting Wednesday completion.
            Tom: Bad news — the CI/CD pipeline upgrade is completely blocked. We've been waiting on
                 AWS IAM credentials from the IT security team for two weeks. Nothing's moving.
            Sarah: That's unacceptable. We need to escalate that to the CTO office today.
            Aisha: Agreed. I'll draft the escalation note now.
            Sarah: Good. Also, the team has decided to postpone the v2 API public launch from
                   July 1st to July 15th to allow time for the security fixes and testing.
            James: That's the right call. I'll update the roadmap doc.
            Sarah: Action items recap: James — JWT patch by Monday. Priya — 80% coverage by Wednesday.
                   Tom — follow up with IT on AWS credentials today. Aisha — escalation email to CTO
                   by 3pm today. James — update roadmap document by EOD.
        """).strip(),
    },
    {
        "id":      "TC-05",
        "name":    "Urgent Escalation",
        "message": (
            "🚨 URGENT — P1 INCIDENT 🚨  "
            "The production payment service has been returning HTTP 500 errors for the last "
            "20 minutes. All customer transactions are failing. Customer support ticket volume "
            "has tripled. We need IMMEDIATE escalation to the DevOps lead, VP Engineering, and CTO. "
            "Revenue impact is estimated at ~$15k/minute. On-call engineer is not responding."
        ),
    },
    {
        "id":      "TC-06",
        "name":    "Ambiguous Request",
        "message": "Hey Nion, things have been a bit off on the project lately. Can you help?",
    },
]


# ══════════════════════════════════════════════════════════════════════════════
# SECTION 12  ·  RUNNER UTILITIES
# ══════════════════════════════════════════════════════════════════════════════

def run_test_case(
    orchestrator: L1Orchestrator,
    test_case: dict[str, str],
    verbose: bool = False,
) -> None:
    """Execute a single test case and print the NION ORCHESTRATION MAP."""
    log = get_logger("runner")
    divider = "█" * _MAP_WIDTH

    print(f"\n{divider}")
    print(f"  TEST CASE {test_case['id']}  ·  {test_case['name']}")
    print(divider)

    preview = test_case["message"]
    if len(preview) > 220:
        preview = preview[:220] + "…"
    print(f"\n  📩 INPUT MESSAGE:\n  {preview}\n")

    input_json = {"message": test_case["message"]}
    result     = orchestrator.orchestrate(input_json)

    print(format_orchestration_map(result))

    if verbose:
        print("\n  📊 RAW JSON PAYLOAD:")
        print(json.dumps(result.model_dump(), indent=2, default=str))

    log.info("Test case %s complete", test_case["id"])


# ══════════════════════════════════════════════════════════════════════════════
# SECTION 13  ·  MAIN ENTRY POINT
# ══════════════════════════════════════════════════════════════════════════════

def main() -> None:
    log = get_logger("main")

    # ── Environment validation ────────────────────────────────────────────────
    provider = os.getenv("LLM_PROVIDER", "nvidia").lower()

    api_key_map = {
        "nvidia": "NVIDIA_API_KEY",
        "openai": "OPENAI_API_KEY",
        "gemini": "GOOGLE_API_KEY",
    }
    if provider not in api_key_map:
        log.error(
            "Unknown LLM_PROVIDER='%s'. Valid values: nvidia (default) | openai | gemini",
            provider,
        )
        sys.exit(1)

    api_key_var = api_key_map[provider]
    if not os.getenv(api_key_var):
        log.error(
            "Required environment variable '%s' is not set for provider='%s'.\n"
            "  Windows PowerShell  : Set-Content .env 'LLM_PROVIDER=nvidia`n%s=nvapi-...' -Encoding UTF8\n"
            "  Or set directly     : $env:%s = 'nvapi-...'",
            api_key_var, provider, api_key_var, api_key_var,
        )
        sys.exit(1)

    log.info("════ NION Orchestration Engine starting  provider=%s ════", provider)

    # ── Build the LLM and orchestrator ───────────────────────────────────────
    llm          = build_llm(temperature=0.2)
    orchestrator = L1Orchestrator(llm)

    # ── Determine which test cases to run ────────────────────────────────────
    # Usage:
    #   python main.py            → runs all 6 test cases
    #   python main.py TC-01      → runs only TC-01
    #   python main.py TC-01 TC-04 TC-05  → runs TC-01, TC-04, TC-05
    #   python main.py --verbose TC-02    → runs TC-02 with raw JSON output
    args    = sys.argv[1:]
    verbose = "--verbose" in args
    if verbose:
        args.remove("--verbose")

    if args:
        selected_ids = set(args)
        cases = [tc for tc in TEST_CASES if tc["id"] in selected_ids]
        if not cases:
            log.error(
                "No test cases match the provided IDs: %s\n  Valid IDs: %s",
                list(selected_ids), [tc["id"] for tc in TEST_CASES],
            )
            sys.exit(1)
    else:
        cases = TEST_CASES  # default: run all 6

    log.info("Running %d test case(s): %s", len(cases), [tc["id"] for tc in cases])

    for tc in cases:
        run_test_case(orchestrator, tc, verbose=verbose)

    log.info("════ All test cases complete ════")


if __name__ == "__main__":
    main()
