import WebSocket from "ws";
import { v4 as uuidv4 } from "uuid";

const WS_URL = "ws://localhost:3055";
const CHANNEL = "ooccx025";

interface FigmaMessage {
  id: string;
  type: string;
  channel: string;
  message: {
    id: string;
    command: string;
    params: any;
  };
}

function hexToRgb(hex: string): { r: number; g: number; b: number; a: number } {
  const cleanHex = hex.replace("#", "");
  const num = parseInt(cleanHex, 16);
  return {
    r: ((num >> 16) & 255) / 255,
    g: ((num >> 8) & 255) / 255,
    b: (num & 255) / 255,
    a: 1.0,
  };
}

async function main() {
  console.log(`Connecting to Figma WebSocket at ${WS_URL}...`);
  const ws = new WebSocket(WS_URL);

  await new Promise<void>((resolve, reject) => {
    ws.on("open", () => {
      console.log("Connected to WebSocket server.");
      resolve();
    });
    ws.on("error", reject);
  });

  // Join channel
  const joinId = uuidv4();
  ws.send(
    JSON.stringify({
      id: joinId,
      type: "join",
      channel: CHANNEL,
      message: {
        id: joinId,
        command: "join",
        params: { channel: CHANNEL, commandId: joinId },
      },
    })
  );
  console.log(`Joined channel ${CHANNEL}`);

  const pendingRequests = new Map<string, { resolve: (res: any) => void; reject: (err: any) => void }>();

  ws.on("message", (data) => {
    try {
      const parsed = JSON.parse(data.toString());
      const responseId = parsed.id || parsed.message?.id || parsed.message?.params?.commandId;
      if (responseId && pendingRequests.has(responseId)) {
        pendingRequests.get(responseId)!.resolve(parsed.result || parsed.message || parsed);
        pendingRequests.delete(responseId);
      }
    } catch (e) {
      // ignore
    }
  });

  async function sendFigmaCommand(command: string, params: any, timeoutMs = 15000): Promise<any> {
    const id = uuidv4();
    params.commandId = id;
    const msg: FigmaMessage = {
      id,
      type: "message",
      channel: CHANNEL,
      message: {
        id,
        command,
        params,
      },
    };

    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        pendingRequests.delete(id);
        reject(new Error(`Timeout waiting for Figma response to ${command}`));
      }, timeoutMs);

      pendingRequests.set(id, {
        resolve: (val) => {
          clearTimeout(timer);
          resolve(val);
        },
        reject: (err) => {
          clearTimeout(timer);
          reject(err);
        },
      });

      ws.send(JSON.stringify(msg));
    });
  }

  console.log("Checking Figma connection status...");
  try {
    const docInfo = await sendFigmaCommand("get_document_info", {}, 5000);
    console.log("Figma client connected! Document info:", docInfo);
  } catch (e) {
    console.log("Figma plugin not responding yet. Script will exit. Run when Figma plugin is active.");
    ws.close();
    return;
  }

  console.log("Drawing Frame 4: 04_Study_Focus_Timer...");
  const f4 = await sendFigmaCommand("create_frame", {
    name: "04_Study_Focus_Timer",
    x: 1314,
    y: 0,
    width: 390,
    height: 844,
    fillColor: hexToRgb("#F9F6F0"),
  });
  console.log("Created Frame 4:", f4);

  console.log("Drawing Frame 5: 05_Companion_And_Shop...");
  const f5 = await sendFigmaCommand("create_frame", {
    name: "05_Companion_And_Shop",
    x: 1752,
    y: 0,
    width: 390,
    height: 844,
    fillColor: hexToRgb("#F9F6F0"),
  });
  console.log("Created Frame 5:", f5);

  ws.close();
  console.log("Done!");
}

main().catch(console.error);
