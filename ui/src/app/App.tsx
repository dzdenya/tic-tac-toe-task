import { useRef, useState } from 'react';
import { GameBoard } from './components/GameBoard';
import { MoveHistory } from './components/MoveHistory';
import { GameStatus } from './components/GameStatus';
import { motion } from 'motion/react';
import { Play } from 'lucide-react';

type Player = 'X' | 'O';
type GameState = 'idle' | 'playing' | 'won' | 'draw';

interface Move {
  player: string;
  position: number;
  timestamp: number;
}

interface SessionMoveResponse {
  turn: number;
  player: Player;
  row: number;
  col: number;
}

interface GameResponse {
  board: (Player | null)[][];
  status: 'IN_PROGRESS' | 'X_WON' | 'O_WON' | 'DRAW';
  winner: Player | null;
}

interface SessionResponse {
  sessionId: string;
  game: GameResponse | null;
  moves: SessionMoveResponse[];
}

declare global {
  interface Window {
    TICTACTOE_API_BASE_URL?: string;
  }
}

const moveDelayMs = 800;
const winningLines = [
  [0, 1, 2],
  [3, 4, 5],
  [6, 7, 8],
  [0, 3, 6],
  [1, 4, 7],
  [2, 5, 8],
  [0, 4, 8],
  [2, 4, 6],
];

export default function App() {
  const [board, setBoard] = useState<(string | null)[]>(Array(9).fill(null));
  const [currentPlayer, setCurrentPlayer] = useState<Player>('X');
  const [gameState, setGameState] = useState<GameState>('idle');
  const [winner, setWinner] = useState<string | null>(null);
  const [winningLine, setWinningLine] = useState<number[] | null>(null);
  const [moveHistory, setMoveHistory] = useState<Move[]>([]);
  const [isSimulating, setIsSimulating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const runIdRef = useRef(0);

  const startSimulation = async () => {
    const runId = runIdRef.current + 1;
    runIdRef.current = runId;

    setBoard(Array(9).fill(null));
    setCurrentPlayer('X');
    setGameState('playing');
    setWinner(null);
    setWinningLine(null);
    setMoveHistory([]);
    setError(null);
    setIsSimulating(true);

    try {
      const session = await postJson<SessionResponse>('/sessions');
      const result = await postJson<SessionResponse>(`/sessions/${session.sessionId}/simulate`);
      const playbackBoard: (string | null)[] = Array(9).fill(null);
      const playbackMoves: Move[] = [];

      for (const move of result.moves) {
        if (runIdRef.current !== runId) {
          return;
        }

        const position = move.row * 3 + move.col;
        playbackBoard[position] = move.player;
        playbackMoves.push({
          player: move.player,
          position,
          timestamp: Date.now(),
        });

        setBoard([...playbackBoard]);
        setMoveHistory([...playbackMoves]);
        setCurrentPlayer(move.player === 'X' ? 'O' : 'X');

        await sleep(moveDelayMs);
      }

      if (runIdRef.current !== runId) {
        return;
      }

      const finalBoard = flattenBoard(result.game?.board);
      const finalWinner = result.game?.winner ?? getWinner(finalBoard);

      setBoard(finalBoard);
      setWinner(finalWinner);
      setWinningLine(finalWinner ? getWinningLine(finalBoard) : null);
      setGameState(result.game?.status === 'DRAW' ? 'draw' : finalWinner ? 'won' : 'idle');
    } catch (caughtError) {
      if (runIdRef.current === runId) {
        setError(caughtError instanceof Error ? caughtError.message : 'Failed to simulate game');
        setGameState('idle');
      }
    } finally {
      if (runIdRef.current === runId) {
        setIsSimulating(false);
      }
    }
  };

  return (
    <div className="size-full flex items-center justify-center bg-gradient-to-br from-blue-50 to-purple-50 p-8">
      <div className="flex flex-col lg:flex-row gap-8 items-start">
        <div className="flex flex-col gap-6 items-center">
          <motion.h1
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            className="text-4xl font-bold text-gray-800"
          >
            Tic Tac Toe Simulation
          </motion.h1>

          <GameStatus
            status={gameState}
            winner={winner}
            currentPlayer={currentPlayer}
          />

          {error && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="max-w-md rounded-lg bg-red-50 px-4 py-3 text-sm font-medium text-red-700 shadow"
            >
              {error}
            </motion.div>
          )}

          <GameBoard board={board} winningLine={winningLine} />

          <div className="flex gap-4">
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={startSimulation}
              disabled={isSimulating}
              className="flex items-center gap-2 px-6 py-3 bg-blue-600 text-white rounded-lg font-semibold shadow-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
            >
              <Play className="w-5 h-5" />
              Start simulation
            </motion.button>
          </div>

          {gameState === 'idle' && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="text-center text-gray-600 text-sm max-w-md"
            >
              <p>Click "Start Simulation" to watch the game being played automatically by the microservices.</p>
            </motion.div>
          )}
        </div>

        <MoveHistory moves={moveHistory} />
      </div>
    </div>
  );
}

async function postJson<T>(path: string): Promise<T> {
  const response = await fetch(`${resolveApiBaseUrl()}${path}`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }

  return response.json();
}

function resolveApiBaseUrl() {
  if (window.TICTACTOE_API_BASE_URL) {
    return window.TICTACTOE_API_BASE_URL;
  }

  if (window.location.protocol.startsWith('http') && window.location.hostname) {
    if (isLocalHost(window.location.hostname)) {
      return `${window.location.protocol}//${window.location.hostname}:8082`;
    }

    if (window.location.pathname.startsWith('/tic-tac-toe')) {
      return `${window.location.origin}/tic-tac-toe-api`;
    }

    return `${window.location.protocol}//${window.location.hostname}:8082`;
  }

  return 'http://localhost:8082';
}

function isLocalHost(hostname: string) {
  return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '[::1]';
}

async function readErrorMessage(response: Response) {
  const fallback = `Request failed with status ${response.status}`;

  try {
    const payload = await response.json();
    return payload.message || fallback;
  } catch {
    const text = await response.text();
    return text || fallback;
  }
}

function flattenBoard(board?: (Player | null)[][]) {
  return board?.flat().map((cell) => cell ?? null) ?? Array(9).fill(null);
}

function getWinner(board: (string | null)[]) {
  const line = getWinningLine(board);
  return line ? board[line[0]] : null;
}

function getWinningLine(board: (string | null)[]) {
  return winningLines.find(([a, b, c]) => {
    return board[a] && board[a] === board[b] && board[a] === board[c];
  }) ?? null;
}

function sleep(ms: number) {
  return new Promise((resolve) => {
    window.setTimeout(resolve, ms);
  });
}
