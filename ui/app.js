const apiBaseUrl = 'http://localhost:8082';

const boardElement = document.querySelector('#board');
const startButton = document.querySelector('#startButton');
const statusText = document.querySelector('#statusText');
const moveHistory = document.querySelector('#moveHistory');
const errorText = document.querySelector('#errorText');

function renderBoard(board = emptyBoard()) {
  boardElement.replaceChildren();

  board.flat().forEach((value) => {
    const cell = document.createElement('div');
    cell.className = 'cell';
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
    item.textContent = `${move.turn}. ${move.player} -> row ${move.row}, col ${move.col} (${move.resultingStatus})`;
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
    const message = await response.text();
    throw new Error(message || `Request failed with status ${response.status}`);
  }

  return response.json();
}

async function startSimulation() {
  startButton.disabled = true;
  errorText.textContent = '';
  statusText.textContent = 'Creating session';
  renderBoard();
  renderMoves();

  try {
    const session = await postJson('/sessions');
    statusText.textContent = 'Simulating';

    const result = await postJson(`/sessions/${session.sessionId}/simulate`);
    statusText.textContent = result.game?.status ?? result.status;
    renderBoard(result.game?.board);
    renderMoves(result.moves);
  } catch (error) {
    statusText.textContent = 'Failed';
    errorText.textContent = error.message;
  } finally {
    startButton.disabled = false;
  }
}

startButton.addEventListener('click', startSimulation);
renderBoard();
