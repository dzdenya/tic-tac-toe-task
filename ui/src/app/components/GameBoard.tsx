import { motion } from 'motion/react';
import { X, Circle } from 'lucide-react';

interface GameBoardProps {
  board: (string | null)[];
  winningLine: number[] | null;
}

const cellSize = 128;
const gapSize = 12;
const cellStep = cellSize + gapSize;
const boardSize = cellSize * 3 + gapSize * 2;
const lineWidth = cellSize * 3 + gapSize * 2 - 24;

export function GameBoard({ board, winningLine }: GameBoardProps) {
  const winningLineStyle = winningLine ? getWinningLineStyle(winningLine) : null;

  return (
    <div className="relative" style={{ width: boardSize, height: boardSize }}>
      <div className="grid grid-cols-3 gap-3">
        {board.map((cell, index) => {
          const isWinningCell = winningLine?.includes(index);

          return (
            <motion.div
              key={index}
              className={`
                flex items-center justify-center
                w-32 h-32
                bg-white border-2 border-gray-300 rounded-lg
                ${isWinningCell ? 'bg-green-100 border-green-500' : ''}
              `}
              initial={{ scale: 0.8, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ duration: 0.3, delay: index * 0.05 }}
            >
              {cell === 'X' && (
                <motion.div
                  initial={{ scale: 0, rotate: -180 }}
                  animate={{ scale: 1, rotate: 0 }}
                  transition={{ type: 'spring', stiffness: 260, damping: 20 }}
                >
                  <X className="w-20 h-20 text-blue-600" strokeWidth={3} />
                </motion.div>
              )}
              {cell === 'O' && (
                <motion.div
                  initial={{ scale: 0, rotate: 180 }}
                  animate={{ scale: 1, rotate: 0 }}
                  transition={{ type: 'spring', stiffness: 260, damping: 20 }}
                >
                  <Circle className="w-20 h-20 text-red-600" strokeWidth={3} />
                </motion.div>
              )}
            </motion.div>
          );
        })}
      </div>

      {winningLineStyle && (
        <motion.div
          className="absolute left-0 top-0 z-10 h-2 rounded-full bg-green-600 shadow-lg shadow-green-500/40"
          initial={{ scaleX: 0, opacity: 0 }}
          animate={{ scaleX: 1, opacity: 1 }}
          transition={{ delay: 0.15, duration: 0.45, ease: 'easeOut' }}
          style={winningLineStyle}
        />
      )}
    </div>
  );
}

function getWinningLineStyle(winningLine: number[]) {
  const [start, , end] = winningLine;
  const startPoint = getCellCenter(start);
  const endPoint = getCellCenter(end);
  const angle = Math.atan2(endPoint.y - startPoint.y, endPoint.x - startPoint.x) * (180 / Math.PI);
  const centerX = (startPoint.x + endPoint.x) / 2;
  const centerY = (startPoint.y + endPoint.y) / 2;

  return {
    width: lineWidth,
    x: centerX - lineWidth / 2,
    y: centerY - 4,
    rotate: angle,
    transformOrigin: 'left center',
  };
}

function getCellCenter(index: number) {
  const row = Math.floor(index / 3);
  const col = index % 3;

  return {
    x: col * cellStep + cellSize / 2,
    y: row * cellStep + cellSize / 2,
  };
}
