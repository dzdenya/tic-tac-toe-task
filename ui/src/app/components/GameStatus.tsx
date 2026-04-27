import { motion } from 'motion/react';
import { Trophy, Pause, Play } from 'lucide-react';

interface GameStatusProps {
  status: 'idle' | 'playing' | 'won' | 'draw';
  winner: string | null;
  currentPlayer: string;
}

export function GameStatus({ status, winner, currentPlayer }: GameStatusProps) {
  const getStatusMessage = () => {
    switch (status) {
      case 'idle':
        return 'Ready to start simulation';
      case 'playing':
        return `Current player: ${currentPlayer}`;
      case 'won':
        return `Player ${winner} wins!`;
      case 'draw':
        return "It's a draw!";
      default:
        return '';
    }
  };

  const getStatusColor = () => {
    switch (status) {
      case 'won':
        return 'text-green-600';
      case 'draw':
        return 'text-yellow-600';
      case 'playing':
        return currentPlayer === 'X' ? 'text-blue-600' : 'text-red-600';
      default:
        return 'text-gray-600';
    }
  };

  const getIcon = () => {
    switch (status) {
      case 'won':
        return <Trophy className="w-6 h-6" />;
      case 'playing':
        return <Play className="w-6 h-6" />;
      default:
        return <Pause className="w-6 h-6" />;
    }
  };

  return (
    <motion.div
      initial={{ y: -20, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      className={`flex items-center gap-3 p-4 bg-white rounded-lg shadow-lg ${getStatusColor()}`}
    >
      {getIcon()}
      <span className="font-semibold text-lg">{getStatusMessage()}</span>
    </motion.div>
  );
}
