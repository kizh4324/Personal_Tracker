import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";

export interface DayItem {
  day: string;
  date: number;
  initial: string;
}

const DAYS: DayItem[] = [
  { day: "Mon", date: 7, initial: "M" },
  { day: "Tue", date: 8, initial: "T" },
  { day: "Wed", date: 9, initial: "W" },
  { day: "Thu", date: 10, initial: "T" },
  { day: "Fri", date: 11, initial: "F" },
  { day: "Sat", date: 12, initial: "S" },
  { day: "Sun", date: 13, initial: "S" },
];

export const DayCapsuleSelector: React.FC<{ compact?: boolean }> = ({ compact = false }) => {
  const [selectedDate, setSelectedDate] = useState<number>(10);

  return (
    <div style={{ display: "flex", gap: "8px", justifyContent: "space-between", width: "100%" }}>
      {DAYS.map((item) => {
        const isSelected = selectedDate === item.date;
        return (
          <motion.button
            key={item.date}
            onClick={() => setSelectedDate(item.date)}
            whileTap={{ scale: 0.94 }}
            style={{
              position: "relative",
              width: compact ? "44px" : "44px",
              height: compact ? "44px" : "72px",
              borderRadius: compact ? "22px" : "22px",
              border: "none",
              backgroundColor: isSelected ? "#1E1E1E" : "#FFFFFF",
              color: isSelected ? "#FFFFFF" : "#1E1E1E",
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              justifyContent: "center",
              cursor: "pointer",
              outline: "none",
              boxShadow: isSelected ? "0 4px 12px rgba(30, 30, 30, 0.18)" : "0 2px 6px rgba(0,0,0,0.03)",
              zIndex: 1,
            }}
          >
            {isSelected && (
              <motion.div
                layoutId="activeDayCapsuleIndicator"
                transition={{
                  type: "spring",
                  stiffness: 380,
                  damping: 28,
                }}
                style={{
                  position: "absolute",
                  inset: 0,
                  backgroundColor: "#1E1E1E",
                  borderRadius: compact ? "22px" : "22px",
                  zIndex: -1,
                }}
              />
            )}
            {!compact ? (
              <>
                <span
                  style={{
                    fontSize: "12px",
                    fontWeight: 500,
                    color: isSelected ? "#FFFFFF" : "#7A7267",
                    marginBottom: "6px",
                  }}
                >
                  {item.day}
                </span>
                <span style={{ fontSize: "16px", fontWeight: 700 }}>{item.date}</span>
              </>
            ) : (
              <span style={{ fontSize: "14px", fontWeight: 600 }}>{item.initial}</span>
            )}
          </motion.button>
        );
      })}
    </div>
  );
};
