import React, { useState } from "react";
import { DayCapsuleSelector } from "./DayCapsuleSelector";
import { TimelineCheckList } from "./TimelineCheckList";
import { SharedElementModal } from "./SharedElementModal";
import { VerticalCapsuleBars } from "./VerticalCapsuleBars";

export const AppPrototype: React.FC = () => {
  const [activeTab, setActiveTab] = useState<"home" | "insights">("home");

  return (
    <div
      style={{
        display: "flex",
        gap: "48px",
        padding: "48px",
        backgroundColor: "#EAE6DE",
        minHeight: "100vh",
        justifyContent: "center",
        alignItems: "flex-start",
        fontFamily: "'Inter', -apple-system, sans-serif",
      }}
    >
      {/* Frame 1: Home Daily Routine */}
      <div
        style={{
          width: "390px",
          height: "844px",
          backgroundColor: "#F9F6F0",
          borderRadius: "44px",
          overflow: "hidden",
          boxShadow: "0 24px 64px rgba(0,0,0,0.12)",
          display: "flex",
          flexDirection: "column",
          padding: "24px 16px",
          position: "relative",
        }}
      >
        {/* Top Greeting Row */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "20px" }}>
          <div>
            <h1 style={{ margin: 0, fontSize: "28px", fontWeight: 700, color: "#1E1E1E" }}>Morning, Budi</h1>
            <p style={{ margin: "4px 0 0 0", fontSize: "14px", color: "#7A7267" }}>Thursday, 10 March, 2025</p>
          </div>
          <div
            style={{
              width: "48px",
              height: "48px",
              borderRadius: "24px",
              backgroundColor: "#FCD9B8",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: "24px",
            }}
          >
            🦊
          </div>
        </div>

        {/* 7-Day Horizontal Capsule Strip */}
        <div style={{ marginBottom: "20px" }}>
          <DayCapsuleSelector />
        </div>

        {/* Hero Reminder Banner Card */}
        <div
          style={{
            height: "148px",
            borderRadius: "24px",
            backgroundColor: "#FCD9B8",
            padding: "20px",
            position: "relative",
            marginBottom: "20px",
            boxShadow: "0 4px 16px rgba(252, 217, 184, 0.4)",
          }}
        >
          <h3 style={{ margin: 0, fontSize: "18px", fontWeight: 700, color: "#1E1E1E" }}>Set the reminder</h3>
          <p style={{ margin: "6px 0 16px 0", fontSize: "12px", color: "#593D24", maxWidth: "200px" }}>
            Never miss your morning routine! Set a reminder to stay on track
          </p>
          <button
            style={{
              height: "36px",
              padding: "0 18px",
              borderRadius: "18px",
              backgroundColor: "#59280E",
              color: "#FFFFFF",
              border: "none",
              fontSize: "13px",
              fontWeight: 700,
              cursor: "pointer",
            }}
          >
            Set Now
          </button>
          <div
            style={{
              position: "absolute",
              right: "20px",
              top: "46px",
              width: "56px",
              height: "56px",
              borderRadius: "28px",
              backgroundColor: "#F26B1D",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: "28px",
              boxShadow: "0 4px 12px rgba(242, 107, 29, 0.3)",
            }}
          >
            🔔
          </div>
        </div>

        {/* Daily Routine Header */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "12px" }}>
          <h2 style={{ margin: 0, fontSize: "20px", fontWeight: 700, color: "#1E1E1E" }}>Daily routine</h2>
          <span style={{ fontSize: "14px", fontWeight: 500, color: "#7A7267", cursor: "pointer" }}>See all</span>
        </div>

        {/* Interactive Timeline Check List */}
        <div style={{ flex: 1, overflowY: "auto" }}>
          <TimelineCheckList />
        </div>

        {/* Shared-Element FAB into New Habit Modal */}
        <SharedElementModal />
      </div>

      {/* Frame 3: Progress & Insights */}
      <div
        style={{
          width: "390px",
          height: "844px",
          backgroundColor: "#F9F6F0",
          borderRadius: "44px",
          overflow: "hidden",
          boxShadow: "0 24px 64px rgba(0,0,0,0.12)",
          display: "flex",
          flexDirection: "column",
          padding: "24px 16px",
          position: "relative",
        }}
      >
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "24px" }}>
          <h1 style={{ margin: 0, fontSize: "24px", fontWeight: 700, color: "#1E1E1E", maxWidth: "260px" }}>
            Your progress and insights
          </h1>
          <button
            style={{
              width: "40px",
              height: "40px",
              borderRadius: "20px",
              backgroundColor: "#FFFFFF",
              border: "none",
              cursor: "pointer",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: "16px",
            }}
          >
            ✕
          </button>
        </div>

        {/* Vertical Capsule Bars & Summary */}
        <VerticalCapsuleBars />
      </div>
    </div>
  );
};

export default AppPrototype;
