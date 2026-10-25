import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";

interface ShopItem {
  id: string;
  name: string;
  cost: number;
  icon: string;
  iconBg: string;
  isEquipped: boolean;
}

const SHOP_ITEMS: ShopItem[] = [
  { id: "1", name: "Cozy Scarf", cost: 25, icon: "🧣", iconBg: "#F5DDF7", isEquipped: true },
  { id: "2", name: "Study Cap", cost: 40, icon: "🎓", iconBg: "#E4F2C3", isEquipped: false },
  { id: "3", name: "Golden Crown", cost: 100, icon: "👑", iconBg: "#FFE8D6", isEquipped: false },
  { id: "4", name: "Warm Coffee", cost: 15, icon: "☕", iconBg: "#FFE8D6", isEquipped: false },
];

export const CompanionStage: React.FC = () => {
  const [coins, setCoins] = useState<number>(145);
  const [items, setItems] = useState<ShopItem[]>(SHOP_ITEMS);
  const [lastEquippedItem, setLastEquippedItem] = useState<string | null>("1");
  const [burstKey, setBurstKey] = useState<number>(0);

  const handleAction = (item: ShopItem) => {
    if (item.isEquipped) return;
    if (coins >= item.cost) {
      setCoins((c) => c - item.cost);
      setItems((prev) =>
        prev.map((i) =>
          i.id === item.id ? { ...i, isEquipped: true } : i
        )
      );
      setLastEquippedItem(item.id);
      setBurstKey((k) => k + 1);
    }
  };

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        width: "100%",
        fontFamily: "'Inter', -apple-system, sans-serif",
      }}
    >
      {/* Header with Coin Balance */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
        <div>
          <h2 style={{ margin: 0, fontSize: "24px", fontWeight: 700, color: "#1E1E1E" }}>
            Your Companion
          </h2>
          <span style={{ fontSize: "13px", color: "#7A7267" }}>
            0 HP loss • Never punished • Growing with you
          </span>
        </div>

        {/* Coin Balance Pill with Burst Animation */}
        <motion.div
          key={burstKey}
          animate={{ scale: [1.0, 1.15, 1.0] }}
          transition={{ type: "spring", stiffness: 400, damping: 18 }}
          style={{
            display: "flex",
            alignItems: "center",
            gap: "6px",
            backgroundColor: "#FFE8D6",
            padding: "8px 16px",
            borderRadius: "999px",
            boxShadow: "0 2px 8px rgba(242, 107, 29, 0.15)",
          }}
        >
          <span style={{ fontSize: "16px" }}>🪙</span>
          <span style={{ fontSize: "15px", fontWeight: 700, color: "#8C3F0B" }}>
            {coins} Coins
          </span>
        </motion.div>
      </div>

      {/* Hero Companion Stage Card */}
      <div
        style={{
          width: "100%",
          borderRadius: "32px",
          backgroundColor: "#FCD9B8",
          padding: "24px 20px",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          marginBottom: "28px",
          boxShadow: "0 8px 24px rgba(252, 217, 184, 0.4)",
          position: "relative",
          overflow: "hidden",
        }}
      >
        {/* Animated Bouncing Fox Pet */}
        <motion.div
          animate={{
            y: [0, -10, 0],
            scale: [1, 1.03, 1],
          }}
          transition={{
            duration: 2.8,
            repeat: Infinity,
            ease: "easeInOut",
          }}
          style={{
            width: "110px",
            height: "110px",
            borderRadius: "55px",
            backgroundColor: "#FFFFFF",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: "56px",
            boxShadow: "0 8px 20px rgba(0,0,0,0.06)",
            marginBottom: "16px",
          }}
        >
          🦊
        </motion.div>

        {/* Level & Mood Badge */}
        <span style={{ fontSize: "18px", fontWeight: 700, color: "#1E1E1E" }}>
          Level 3 • Happy Fox
        </span>
        <span style={{ fontSize: "13px", color: "#593D24", marginTop: "2px" }}>
          80 / 100 Energy to Level 4
        </span>

        {/* Lime Energy Progress Bar */}
        <div
          style={{
            width: "220px",
            height: "8px",
            borderRadius: "4px",
            backgroundColor: "#FFFFFF",
            marginTop: "12px",
            overflow: "hidden",
          }}
        >
          <motion.div
            initial={{ width: 0 }}
            animate={{ width: "80%" }}
            transition={{ duration: 1.2, ease: "easeOut" }}
            style={{
              height: "100%",
              backgroundColor: "#A3D929",
              borderRadius: "4px",
            }}
          />
        </div>
      </div>

      {/* Wardrobe & Shop Section */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
        <h3 style={{ margin: 0, fontSize: "18px", fontWeight: 700, color: "#1E1E1E" }}>
          Companion Wardrobe & Items
        </h3>
        <span style={{ fontSize: "13px", fontWeight: 600, color: "#7A7267", cursor: "pointer" }}>
          View all
        </span>
      </div>

      {/* 2x2 Grid of Shop Cards */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: "14px",
          width: "100%",
        }}
      >
        {items.map((item) => (
          <motion.div
            key={item.id}
            whileHover={{ y: -3 }}
            style={{
              backgroundColor: "#FFFFFF",
              borderRadius: "20px",
              padding: "16px",
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              boxShadow: "0 2px 8px rgba(0,0,0,0.04)",
            }}
          >
            <div
              style={{
                width: "48px",
                height: "48px",
                borderRadius: "14px",
                backgroundColor: item.iconBg,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontSize: "24px",
                marginBottom: "10px",
              }}
            >
              {item.icon}
            </div>
            <span style={{ fontSize: "14px", fontWeight: 600, color: "#1E1E1E", marginBottom: "2px" }}>
              {item.name}
            </span>
            <span style={{ fontSize: "12px", color: "#7A7267", marginBottom: "12px" }}>
              {item.cost} Coins
            </span>

            {/* Action CTA */}
            <motion.button
              whileTap={{ scale: 0.94 }}
              onClick={() => handleAction(item)}
              style={{
                width: "100%",
                height: "36px",
                borderRadius: "18px",
                border: "none",
                backgroundColor: item.isEquipped ? "#1E1E1E" : "#F26B1D",
                color: "#FFFFFF",
                fontSize: "13px",
                fontWeight: 700,
                cursor: item.isEquipped ? "default" : "pointer",
              }}
            >
              {item.isEquipped ? "Equipped" : `${item.cost} Coins`}
            </motion.button>
          </motion.div>
        ))}
      </div>
    </div>
  );
};
