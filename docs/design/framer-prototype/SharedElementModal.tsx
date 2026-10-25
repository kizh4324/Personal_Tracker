import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { DayCapsuleSelector } from "./DayCapsuleSelector";

export const SharedElementModal: React.FC = () => {
  const [isOpen, setIsOpen] = useState<boolean>(false);

  return (
    <>
      {/* Floating Action Button (FAB) */}
      {!isOpen && (
        <motion.button
          layoutId="sharedFabModalCard"
          onClick={() => setIsOpen(true)}
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.92 }}
          transition={{
            type: "spring",
            stiffness: 320,
            damping: 26,
          }}
          style={{
            position: "fixed",
            bottom: "32px",
            right: "32px",
            width: "56px",
            height: "56px",
            borderRadius: "28px",
            backgroundColor: "#59280E",
            color: "#FFFFFF",
            border: "none",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: "28px",
            cursor: "pointer",
            boxShadow: "0 8px 24px rgba(89, 40, 14, 0.35)",
            zIndex: 100,
          }}
        >
          +
        </motion.button>
      )}

      {/* Shared-Element Modal Sheet */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            style={{
              position: "fixed",
              inset: 0,
              backgroundColor: "rgba(0, 0, 0, 0.4)",
              backdropFilter: "blur(4px)",
              display: "flex",
              alignItems: "flex-end",
              justifyContent: "center",
              zIndex: 110,
            }}
          >
            <motion.div
              layoutId="sharedFabModalCard"
              transition={{
                type: "spring",
                stiffness: 320,
                damping: 26,
              }}
              style={{
                width: "100%",
                maxWidth: "390px",
                height: "85vh",
                maxHeight: "844px",
                backgroundColor: "#F9F6F0",
                borderTopLeftRadius: "32px",
                borderTopRightRadius: "32px",
                padding: "24px",
                display: "flex",
                flexDirection: "column",
                overflowY: "auto",
                position: "relative",
              }}
            >
              {/* Header */}
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <h2 style={{ margin: 0, fontSize: "24px", fontWeight: 700, color: "#1E1E1E" }}>New habit</h2>
                <motion.button
                  whileTap={{ scale: 0.9 }}
                  onClick={() => setIsOpen(false)}
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
                    fontWeight: 600,
                  }}
                >
                  ✕
                </motion.button>
              </div>

              {/* Lime Hero Container */}
              <div style={{ display: "flex", justifyContent: "center", margin: "24px 0" }}>
                <motion.div
                  initial={{ scale: 0.8, opacity: 0 }}
                  animate={{ scale: 1.0, opacity: 1 }}
                  transition={{ delay: 0.15, type: "spring", stiffness: 300, damping: 20 }}
                  style={{
                    width: "120px",
                    height: "120px",
                    borderRadius: "32px",
                    backgroundColor: "#A3D929",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  <div
                    style={{
                      width: "64px",
                      height: "64px",
                      borderRadius: "16px",
                      backgroundColor: "#FFFFFF",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      fontSize: "32px",
                    }}
                  >
                    📅
                  </div>
                </motion.div>
              </div>

              {/* Form Input */}
              <div style={{ marginBottom: "20px" }}>
                <label style={{ display: "block", fontSize: "14px", fontWeight: 600, color: "#7A7267", marginBottom: "8px" }}>
                  Name your habit
                </label>
                <div
                  style={{
                    height: "52px",
                    backgroundColor: "#FFFFFF",
                    borderRadius: "16px",
                    padding: "0 18px",
                    display: "flex",
                    alignItems: "center",
                    fontSize: "15px",
                    fontWeight: 600,
                    color: "#1E1E1E",
                  }}
                >
                  Morning Meditations
                </div>
              </div>

              {/* Goals */}
              <div style={{ marginBottom: "20px" }}>
                <label style={{ display: "block", fontSize: "14px", fontWeight: 600, color: "#7A7267", marginBottom: "8px" }}>
                  Set a goal
                </label>
                <div style={{ display: "flex", gap: "12px" }}>
                  <div
                    style={{
                      flex: 1,
                      height: "52px",
                      backgroundColor: "#FFFFFF",
                      borderRadius: "16px",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                      padding: "0 16px",
                      fontSize: "14px",
                      fontWeight: 500,
                    }}
                  >
                    <span>Add date</span>
                    <span>📅</span>
                  </div>
                  <div
                    style={{
                      flex: 1,
                      height: "52px",
                      backgroundColor: "#FFFFFF",
                      borderRadius: "16px",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                      padding: "0 16px",
                      fontSize: "14px",
                      fontWeight: 500,
                    }}
                  >
                    <span>Add amount</span>
                    <span>⏱️</span>
                  </div>
                </div>
              </div>

              {/* Repeat Days */}
              <div style={{ marginBottom: "20px" }}>
                <label style={{ display: "block", fontSize: "14px", fontWeight: 600, color: "#7A7267", marginBottom: "8px" }}>
                  Repeat days
                </label>
                <DayCapsuleSelector compact />
              </div>

              {/* Reminders Toggle */}
              <div
                style={{
                  height: "60px",
                  backgroundColor: "#FFFFFF",
                  borderRadius: "18px",
                  padding: "0 18px",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  marginBottom: "28px",
                }}
              >
                <span style={{ fontSize: "15px", fontWeight: 600, color: "#1E1E1E" }}>Get reminders</span>
                <div
                  style={{
                    width: "48px",
                    height: "28px",
                    borderRadius: "14px",
                    backgroundColor: "#F26B1D",
                    position: "relative",
                    padding: "3px",
                  }}
                >
                  <motion.div
                    layout
                    style={{
                      width: "22px",
                      height: "22px",
                      borderRadius: "11px",
                      backgroundColor: "#FFFFFF",
                      marginLeft: "auto",
                    }}
                  />
                </div>
              </div>

              {/* CTA */}
              <motion.button
                whileTap={{ scale: 0.97 }}
                onClick={() => setIsOpen(false)}
                style={{
                  height: "56px",
                  backgroundColor: "#F26B1D",
                  borderRadius: "999px",
                  color: "#FFFFFF",
                  fontSize: "16px",
                  fontWeight: 700,
                  border: "none",
                  cursor: "pointer",
                  marginTop: "auto",
                  boxShadow: "0 6px 18px rgba(242, 107, 29, 0.35)",
                }}
              >
                Save Habit
              </motion.button>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
};
