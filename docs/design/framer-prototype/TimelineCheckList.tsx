import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";

interface RoutineItem {
  id: string;
  name: string;
  streak: string;
  icon: string;
  iconBg: string;
  completed: boolean;
}

const INITIAL_ROUTINES: RoutineItem[] = [
  { id: "1", name: "Drink a glass of water", streak: "Streak 3 days • 5 min", icon: "💧", iconBg: "#FFE8D6", completed: true },
  { id: "2", name: "Meditate to relax", streak: "Streak 6 days • 15 min", icon: "🧘", iconBg: "#E4F2C3", completed: true },
  { id: "3", name: "Stretch for 10 minutes", streak: "Streak 5 days • 10 min", icon: "🤸", iconBg: "#F5DDF7", completed: false },
  { id: "4", name: "Go for a short walk", streak: "Streak 3 days • 15 min", icon: "👟", iconBg: "#FFE8D6", completed: false },
];

export const TimelineCheckList: React.FC = () => {
  const [routines, setRoutines] = useState<RoutineItem[]>(INITIAL_ROUTINES);

  const toggleRoutine = (id: string) => {
    setRoutines((prev) =>
      prev.map((r) => (r.id === id ? { ...r, completed: !r.completed } : r))
    );
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "12px", width: "100%" }}>
      {routines.map((item, idx) => (
        <div key={item.id} style={{ display: "flex", alignItems: "center", gap: "14px" }}>
          {/* Timeline Node with Spring Burst Animation */}
          <div style={{ position: "relative", display: "flex", alignItems: "center", justifyContent: "center" }}>
            {idx < routines.length - 1 && (
              <motion.div
                initial={false}
                animate={{
                  backgroundColor: item.completed && routines[idx + 1].completed ? "#F26B1D" : "#D9D2C9",
                }}
                transition={{ duration: 0.3 }}
                style={{
                  position: "absolute",
                  top: "20px",
                  bottom: "-32px",
                  width: "2px",
                  zIndex: 0,
                }}
              />
            )}
            <motion.button
              onClick={() => toggleRoutine(item.id)}
              whileTap={{ scale: 0.85 }}
              animate={
                item.completed
                  ? { scale: [1.0, 1.25, 1.0], backgroundColor: "#F26B1D" }
                  : { scale: 1.0, backgroundColor: "#F9F6F0" }
              }
              transition={{
                type: "spring",
                stiffness: 450,
                damping: 20,
              }}
              style={{
                width: "20px",
                height: "20px",
                borderRadius: "10px",
                border: item.completed ? "none" : "2px solid #D9D2C9",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                cursor: "pointer",
                padding: 0,
                outline: "none",
                zIndex: 1,
              }}
            >
              <AnimatePresence>
                {item.completed && (
                  <motion.span
                    initial={{ opacity: 0, scale: 0.5 }}
                    animate={{ opacity: 1, scale: 1.0 }}
                    exit={{ opacity: 0, scale: 0.5 }}
                    transition={{ duration: 0.15 }}
                    style={{ color: "#FFFFFF", fontSize: "11px", fontWeight: "bold" }}
                  >
                    ✓
                  </motion.span>
                )}
              </AnimatePresence>
            </motion.button>
          </div>

          {/* Routine Card */}
          <motion.div
            layout
            whileTap={{ scale: 0.98 }}
            onClick={() => toggleRoutine(item.id)}
            style={{
              flex: 1,
              height: "76px",
              backgroundColor: "#FFFFFF",
              borderRadius: "20px",
              display: "flex",
              alignItems: "center",
              padding: "0 16px",
              gap: "14px",
              cursor: "pointer",
              boxShadow: "0 2px 8px rgba(0,0,0,0.03)",
            }}
          >
            <div
              style={{
                width: "44px",
                height: "44px",
                borderRadius: "12px",
                backgroundColor: item.iconBg,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontSize: "20px",
              }}
            >
              {item.icon}
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
              <span
                style={{
                  fontSize: "15px",
                  fontWeight: 600,
                  color: "#1E1E1E",
                  textDecoration: item.completed ? "line-through" : "none",
                  opacity: item.completed ? 0.75 : 1.0,
                }}
              >
                {item.name}
              </span>
              <span style={{ fontSize: "12px", color: "#7A7267" }}>{item.streak}</span>
            </div>
          </motion.div>
        </div>
      ))}
    </div>
  );
};
