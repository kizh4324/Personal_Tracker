import React, { useEffect, useState } from "react";
import { motion, useSpring, useTransform } from "framer-motion";

interface BarMetric {
  name: string;
  pct: number;
  color: string;
}

const BARS: BarMetric[] = [
  { name: "Walking", pct: 48, color: "#4B322C" },
  { name: "Running", pct: 33, color: "#8C3F0B" },
  { name: "Meditation", pct: 27, color: "#7FA62D" },
  { name: "Drink", pct: 40, color: "#DC75C2" },
];

export const VerticalCapsuleBars: React.FC = () => {
  const [mounted, setMounted] = useState<boolean>(false);
  const countSpring = useSpring(0, { stiffness: 60, damping: 20 });
  const displayScore = useTransform(countSpring, (val) => Math.round(val));

  useEffect(() => {
    setMounted(true);
    countSpring.set(842);
  }, [countSpring]);

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "24px", width: "100%" }}>
      {/* 4 Staggered Bars */}
      <div style={{ display: "flex", justifyContent: "space-between", height: "280px", width: "100%" }}>
        {BARS.map((bar, index) => {
          const targetHeight = (bar.pct / 100) * 280;
          return (
            <div
              key={bar.name}
              style={{
                position: "relative",
                width: "72px",
                height: "280px",
                borderRadius: "36px",
                backgroundColor: "#EBE5DC",
                overflow: "hidden",
                display: "flex",
                flexDirection: "column",
                justifyContent: "space-between",
                alignItems: "center",
                padding: "16px 0",
              }}
            >
              {/* Top Percentage Label */}
              <span style={{ fontSize: "14px", fontWeight: 700, color: "#1E1E1E", zIndex: 2 }}>
                {bar.pct}%
              </span>

              {/* Animated Growing Fill Capsule */}
              <motion.div
                initial={{ height: 0 }}
                animate={mounted ? { height: targetHeight } : { height: 0 }}
                transition={{
                  type: "spring",
                  stiffness: 220,
                  damping: 24,
                  delay: index * 0.08,
                }}
                style={{
                  position: "absolute",
                  bottom: 0,
                  left: 0,
                  right: 0,
                  borderRadius: "36px",
                  backgroundColor: bar.color,
                  zIndex: 1,
                }}
              />

              {/* Bottom Activity Name Label */}
              <span
                style={{
                  fontSize: bar.name === "Meditation" ? "10px" : "12px",
                  fontWeight: 600,
                  color: "#FFFFFF",
                  zIndex: 2,
                }}
              >
                {bar.name}
              </span>
            </div>
          );
        })}
      </div>

      {/* Summary Card with Animated Score Counter */}
      <div
        style={{
          backgroundColor: "#FFFFFF",
          borderRadius: "28px",
          padding: "24px",
          boxShadow: "0 4px 16px rgba(0,0,0,0.04)",
        }}
      >
        <span style={{ fontSize: "13px", fontWeight: 500, color: "#7A7267" }}>
          Points Earned / For this week
        </span>
        <div style={{ fontSize: "32px", fontWeight: 700, color: "#8C3F0B", margin: "6px 0 20px 0" }}>
          <motion.span>{displayScore}</motion.span> Points
        </div>

        {/* 3 Metrics Row */}
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "24px" }}>
          <div>
            <div style={{ fontSize: "18px", fontWeight: 700, color: "#1E1E1E" }}>440 lb</div>
            <div style={{ fontSize: "12px", color: "#7A7267", marginTop: "2px" }}>Weight</div>
          </div>
          <div>
            <div style={{ fontSize: "18px", fontWeight: 700, color: "#1E1E1E" }}>200 ft</div>
            <div style={{ fontSize: "12px", color: "#7A7267", marginTop: "2px" }}>Elevation</div>
          </div>
          <div>
            <div style={{ fontSize: "18px", fontWeight: 700, color: "#1E1E1E" }}>7h 30m</div>
            <div style={{ fontSize: "12px", color: "#7A7267", marginTop: "2px" }}>Total time</div>
          </div>
        </div>

        {/* Share Progress CTA */}
        <motion.button
          whileTap={{ scale: 0.97 }}
          style={{
            width: "100%",
            height: "52px",
            borderRadius: "26px",
            backgroundColor: "#F26B1D",
            color: "#FFFFFF",
            fontSize: "16px",
            fontWeight: 700,
            border: "none",
            cursor: "pointer",
            boxShadow: "0 6px 18px rgba(242, 107, 29, 0.3)",
          }}
        >
          Share Progress
        </motion.button>
      </div>
    </div>
  );
};
