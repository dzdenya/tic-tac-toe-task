import { motion } from 'motion/react';

interface Move {
  player: string;
  position: number;
  timestamp: number;
}

interface MoveHistoryProps {
  moves: Move[];
}

export function MoveHistory({ moves }: MoveHistoryProps) {
  const getPositionLabel = (pos: number) => {
    const row = Math.floor(pos / 3) + 1;
    const col = (pos % 3) + 1;
    return `Row ${row}, Col ${col}`;
  };

  return (
    <div className="bg-white rounded-lg shadow-lg p-6 w-96 max-h-[520px] overflow-y-auto">
      <h3 className="font-semibold text-lg mb-4 text-gray-800">Move History</h3>
      {moves.length === 0 ? (
        <p className="text-gray-500 text-sm">No moves yet</p>
      ) : (
        <div className="space-y-2">
          {moves.map((move, index) => (
            <motion.div
              key={index}
              initial={{ x: -20, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              transition={{ duration: 0.3 }}
              className="flex items-center justify-between p-2 bg-gray-50 rounded"
            >
              <span className="text-sm">
                <span className={`font-bold ${move.player === 'X' ? 'text-blue-600' : 'text-red-600'}`}>
                  {move.player}
                </span>
                {' '}→ {getPositionLabel(move.position)}
              </span>
              <span className="text-xs text-gray-500">
                Move #{index + 1}
              </span>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
}
