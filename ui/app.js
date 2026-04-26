const apiBaseUrl = resolveApiBaseUrl();

const boardElement = document.querySelector('#board');
const startButton = document.querySelector('#startButton');
const statusText = document.querySelector('#statusText');
const sessionText = document.querySelector('#sessionText');
const moveHistory = document.querySelector('#moveHistory');
const errorText = document.querySelector('#errorText');
const moveDelayMs = 700;

function resolveApiBaseUrl() {
  if (window.TICTACTOE_API_BASE_URL) {
    return window.TICTACTOE_API_BASE_URL;
  }

  if (window.location.protocol.startsWith('http') && window.location.hostname) {
    return `${window.location.protocol}//${window.location.hostname}:8082`;
  }

  return 'http://localhost:8082';
}

function renderBoard(board = emptyBoard()) {
  boardElement.replaceChildren();

  board.flat().forEach((value) => {
    const cell = document.createElement('div');
    cell.className = value ? `cell ${value.toLowerCase()}` : 'cell';
    cell.textContent = value ?? '';
    boardElement.append(cell);
  });
}

function emptyBoard() {
  return [
    [null, null, null],
    [null, null, null],
    [null, null, null],
  ];
}

function renderMoves(moves = []) {
  moveHistory.replaceChildren();

  moves.forEach((move) => {
    const item = document.createElement('li');
    item.textContent = `${move.player} -> row ${move.row}, col ${move.col} (${formatStatus(move.resultingStatus)})`;
    moveHistory.append(item);
  });
}

async function postJson(path) {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    const message = await readErrorMessage(response);
    throw new Error(message);
  }

  return response.json();
}

async function readErrorMessage(response) {
  const fallback = `Request failed with status ${response.status}`;

  try {
    const payload = await response.json();
    return payload.message || fallback;
  } catch {
    const text = await response.text();
    return text || fallback;
  }
}

async function startSimulation() {
  startButton.disabled = true;
  errorText.textContent = '';
  statusText.textContent = 'Creating session';
  sessionText.textContent = 'Not started';
  renderBoard();
  renderMoves();

  try {
    const session = await postJson('/sessions');
    sessionText.textContent = session.sessionId;
    statusText.textContent = 'Simulating';

    const result = await postJson(`/sessions/${session.sessionId}/simulate`);
    await playMoves(result.moves);
    statusText.textContent = formatStatus(result.game?.status ?? result.status, result.game?.winner);
    renderBoard(result.game?.board);
  } catch (error) {
    statusText.textContent = 'Failed';
    errorText.textContent = error.message;
  } finally {
    startButton.disabled = false;
  }
}

async function playMoves(moves = []) {
  const board = emptyBoard();
  renderBoard(board);
  renderMoves();

  for (const move of moves) {
    statusText.textContent = `Turn ${move.turn}: ${move.player}`;
    board[move.row][move.col] = move.player;
    renderBoard(board);
    renderMoves(moves.slice(0, move.turn));
    await sleep(moveDelayMs);
  }
}

function sleep(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

function formatStatus(status, winner) {
  if (winner) {
    return `${winner} won`;
  }
  if (status === 'DRAW') {
    return 'Draw';
  }
  return status?.replaceAll('_', ' ') ?? 'Unknown';
}

startButton.addEventListener('click', startSimulation);
renderBoard();
