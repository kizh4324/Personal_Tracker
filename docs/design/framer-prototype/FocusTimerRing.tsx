import React, { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";

export const FocusTimerRing: React.FC = () => {
  const [isActive, setIsActive] = useState<boolean>(true);
  const [secondsRemaining, setSecondsRemaining] = useState<number>(24 * 60 + 18); // 24:18
  const [selectedIntensity, setSelectedIntensity] = useState<string>("Pomodoro");

  const totalSeconds = 25 * 60;
  const progress = (totalSeconds - secondsRemaining) / totalSeconds;
  const radius = 100;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference * (1 - progress);

  useEffect(() => {
    if (!isActive) return;
    const interval = setInterval(() => {
      setSecondsRemaining((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(interval);
  }, [isActive]);

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60).toString().padStart(2, "0");
    const s = (secs % 60).toString().padStart(2, "0");
    return `${m}:${s}`;
  };

  const intensities = ["Deep Work", "Pomodoro", "Flow State"];

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        width: "100%",
        fontFamily: "'Inter', -apple-system, sans-serif",
      }}
    >
      {/* Session Title Header */}
      <div style={{ textAlign: "center", marginBottom: "28px" }}>
        <h2 style={{ margin: 0, fontSize: "24px", fontWeight: 700, color: "#1E1E1E" }}>
          Focus Session
        </h2>
        <p style={{ margin: "4px 0 0 0", fontSize: "14px", color: "#7A7267" }}>
          Physics Exam Revision • 25 min
        </p>
      </div>

      {/* Breathing Timer Ring Container */}
      <motion.div
        animate={
          isActive
            ? {
                scale: [1.0, 1.025, 1.0],
                boxShadow: [
                  "0 8px 32px rgba(242, 107, 29, 0.12)",
                  "0 12px 48px rgba(242, 107, 29, 0.22)",
                  "0 8px 32px rgba(242, 107, 29, 0.12)",
                ],
              }
            : { scale: 1.0, boxShadow: "0 4px 16px rgba(0,0,0,0.06)" }
        }
        transition={{
          duration: 3.5,
          repeat: Infinity,
          ease: "easeInOut",
        }}
        style={{
          position: "relative",
          width: "260px",
          height: "260px",
          borderRadius: "130px",
          backgroundColor: "#FFFFFF",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          marginBottom: "28px",
        }}
      >
        <svg width="240" height="240" style={{ transform: "rotate(-90deg)" }}>
          {/* Hatched/Sand background track */}
          <circle
            cx="120"
            cy="120"
            r={radius}
            stroke="#EBE5DC"
            strokeWidth="12"
            fill="transparent"
          />
          {/* Vibrant Tangerine progress arc */}
          <motion.circle
            cx="120"
            cy="120"
            r={radius}
            stroke="#F26B1D"
            strokeWidth="12"
            strokeDasharray={circumference}
            animate={{ strokeDashoffset }}
            transition={{ type: "spring", stiffness: 80, damping: 20 }}
            strokeLinecap="round"
            fill="transparent"
          />
        </svg>

        {/* Center Time & Reward Badge */}
        <div
          style={{
            position: "absolute",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
          }}
        >
          <motion.span
            key={secondsRemaining}
            initial={{ opacity: 0.9, y: 1 }}
            animate={{ opacity: 1, y: 0 }}
            style={{ fontSize: "44px", fontWeight: 700, color: "#1E1E1E" }}
          >
            {formatTime(secondsRemaining)}
          </motion.span>
          <span style={{ fontSize: "13px", fontWeight: 500, color: "#7A7267", marginTop: "2px" }}>
            {isActive ? "Deep focus in progress..." : "Session paused"}
          </span>
          <div
            style={{
              marginTop: "8px",
              padding: "4px 12px",
              borderRadius: "999px",
              backgroundColor: "#FFE8D6",
              color: "#F26B1D",
              fontSize: "12px",
              fontWeight: 700,
            }}
          >
            +10 Coins Reward
          </div>
        </div>
      </motion.div>

      {/* Intensity Selector Pills */}
      <div
        style={{
          display: "flex",
          gap: "8px",
          backgroundColor: "#FFFFFF",
          padding: "6px",
          borderRadius: "20px",
          marginBottom: "32px",
          boxShadow: "0 2px 8px rgba(0,0,0,0.04)",
        }}
      >
        {intensities.map((item) => {
          const isSelected = selectedIntensity === item;
          return (
            <motion.button
              key={item}
              onClick={() => setSelectedIntensity(item)}
              whileTap={{ scale: 0.94 }}
              style={{
                padding: "8px 16px",
                borderRadius: "14px",
                border: "none",
                backgroundColor: isSelected ? "#1E1E1E" : "transparent",
                color: isSelected ? "#FFFFFF" : "#7A7267",
                fontSize: "13px",
                fontWeight: 600,
                cursor: "pointer",
              }}
            >
              {item}
            </motion.button>
          );
        })}
      </div>

      {/* Tactile Control Buttons */}
      <div style={{ display: "flex", gap: "12px", width: "100%", maxWidth: "350px" }}>
        <motion.button
          onClick={() => setIsActive(!isActive)}
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.96 }}
          transition={{ type: "spring", stiffness: 350, damping: 25 }}
          style={{
            flex: 1.3,
            height: "56px",
            borderRadius: "28px",
            backgroundColor: "#59280E",
            color: "#FFFFFF",
            border: "none",
            fontSize: "16px",
            fontWeight: 700,
            cursor: "pointer",
            boxShadow: "0 6px 18px rgba(89, 40, 14, 0.25)",
          }}
        >
          {isActive ? "Pause Session" : "Resume Session"}
        </motion.button>

        <motion.button
          onClick={() => setSecondsRemaining(25 * 60)}
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.96 }}
          transition={{ type: "spring", stiffness: 350, damping: 25 }}
          style={{
            flex: 0.9,
            height: "56px",
            borderRadius: "28px",
            backgroundColor: "#FFFFFF",
            color: "#7A7267",
            border: "2px solid #D9D2C9",
            fontSize: "15px",
            fontWeight: 600,
            cursor: "pointer",
          }}
        >
          End Early
        </motion.button>
      </div>
    </div>
  );
};
