class MazeMaster {
  constructor() {
    this.canvas = document.getElementById('mazeCanvas');
    this.ctx = this.canvas.getContext('2d');

    // ── Shared state ────────────────────────────────────────────────────────
    this.maze = null;
    this.solution = null;
    this.cellSize = 20;
    this.animationFrame = 0;
    this.isAnimating = false;
    this.solveTimeMs = 0;

    // ── Mode ─────────────────────────────────────────────────────────────────
    this.mode = 'algorithm'; // 'algorithm' | 'manual'

    // ── Manual play state ────────────────────────────────────────────────────
    this.playerPos    = { row: 0, col: 0 };
    this.steps        = 0;
    this.timerMs      = 0;
    this.timerInterval = null;
    this.playActive   = false;  // timer is running
    this.playerWon    = false;
    this.visitedCells = new Set(); // track trail — "row,col"

    this.initializeElements();
    this.bindEvents();
    this.loadHighScores();
    this.loadTopAttempts();
  }

  // ════════════════════════════════════════════════════════════════════════════
  // INIT
  // ════════════════════════════════════════════════════════════════════════════

  initializeElements() {
    this.el = {
      // Algorithm
      tabAlgorithm:      document.getElementById('tabAlgorithm'),
      tabManual:         document.getElementById('tabManual'),
      algorithmPanel:    document.getElementById('algorithmPanel'),
      manualPanel:       document.getElementById('manualPanel'),

      generateBtn:       document.getElementById('generateBtn'),
      solveBtn:          document.getElementById('solveBtn'),
      submitScoreBtn:    document.getElementById('submitScoreBtn'),
      rowsInput:         document.getElementById('rows'),
      colsInput:         document.getElementById('cols'),
      algorithmSelect:   document.getElementById('algorithm'),
      showSolutionCheck: document.getElementById('showSolution'),
      animatePathCheck:  document.getElementById('animatePath'),
      status:            document.getElementById('status'),
      timer:             document.getElementById('timer'),
      mazeSize:          document.getElementById('mazeSize'),
      pathLength:        document.getElementById('pathLength'),
      solveTime:         document.getElementById('solveTime'),
      scoreModal:        document.getElementById('scoreModal'),
      playerNameInput:   document.getElementById('playerName'),
      confirmScoreBtn:   document.getElementById('confirmScore'),
      cancelScoreBtn:    document.getElementById('cancelScore'),
      scoresContainer:   document.getElementById('scoresContainer'),

      // Manual
      manualGenerateBtn: document.getElementById('manualGenerateBtn'),
      manualRowsInput:   document.getElementById('manualRows'),
      manualColsInput:   document.getElementById('manualCols'),
      startPlayBtn:      document.getElementById('startPlayBtn'),
      giveUpBtn:         document.getElementById('giveUpBtn'),
      playTimerEl:       document.getElementById('playTimer'),
      playStepsEl:       document.getElementById('playSteps'),
      attemptsContainer: document.getElementById('attemptsContainer'),
      playerLegendItem:  document.getElementById('playerLegendItem'),

      // Win modal
      attemptModal:         document.getElementById('attemptModal'),
      winTime:              document.getElementById('winTime'),
      winSteps:             document.getElementById('winSteps'),
      attemptPlayerName:    document.getElementById('attemptPlayerName'),
      confirmAttemptBtn:    document.getElementById('confirmAttempt'),
      cancelAttemptBtn:     document.getElementById('cancelAttempt'),

      // Give-up modal
      giveUpModal:          document.getElementById('giveUpModal'),
      tryAgainBtn:          document.getElementById('tryAgainBtn'),
      tryAlgorithmBtn:      document.getElementById('tryAlgorithmBtn'),
      newMazeBtn:           document.getElementById('newMazeBtn'),
    };
  }

  bindEvents() {
    // Mode tabs
    this.el.tabAlgorithm.addEventListener('click', () => this.switchMode('algorithm'));
    this.el.tabManual.addEventListener('click',    () => this.switchMode('manual'));

    // Algorithm mode
    this.el.generateBtn.addEventListener('click',      () => this.generateMaze('algorithm'));
    this.el.solveBtn.addEventListener('click',          () => this.solveMaze());
    this.el.submitScoreBtn.addEventListener('click',    () => this.showScoreModal());
    this.el.confirmScoreBtn.addEventListener('click',   () => this.submitScore());
    this.el.cancelScoreBtn.addEventListener('click',    () => this.hideScoreModal());
    this.el.showSolutionCheck.addEventListener('change',() => this.render());
    this.el.animatePathCheck.addEventListener('change', () => {
      if (this.solution) this.render();
    });

    // Manual mode
    this.el.manualGenerateBtn.addEventListener('click', () => this.generateMaze('manual'));
    this.el.startPlayBtn.addEventListener('click',      () => this.startManualPlay());
    this.el.giveUpBtn.addEventListener('click',         () => this.giveUp());

    // Win modal
    this.el.confirmAttemptBtn.addEventListener('click', () => this.submitAttempt());
    this.el.cancelAttemptBtn.addEventListener('click',  () => this.hideAttemptModal());

    // Give-up modal
    this.el.tryAgainBtn.addEventListener('click',      () => this.handleTryAgain());
    this.el.tryAlgorithmBtn.addEventListener('click',  () => this.handleTryAlgorithm());
    this.el.newMazeBtn.addEventListener('click',       () => this.handleNewMaze());

    // Arrow key input
    document.addEventListener('keydown', (e) => this.handleKeyDown(e));
  }

  // ════════════════════════════════════════════════════════════════════════════
  // MODE SWITCHING
  // ════════════════════════════════════════════════════════════════════════════

  switchMode(mode) {
    // Stop any active manual session when leaving manual mode
    if (this.mode === 'manual' && mode !== 'manual') {
      this.stopTimer();
    }

    this.mode = mode;

    if (mode === 'algorithm') {
      this.el.tabAlgorithm.classList.add('active');
      this.el.tabManual.classList.remove('active');
      this.el.algorithmPanel.classList.remove('hidden');
      this.el.manualPanel.classList.add('hidden');
      this.el.playerLegendItem.style.display = 'none';
      // Re-render without player if maze exists
      if (this.maze) this.render();
    } else {
      this.el.tabManual.classList.add('active');
      this.el.tabAlgorithm.classList.remove('active');
      this.el.manualPanel.classList.remove('hidden');
      this.el.algorithmPanel.classList.add('hidden');
      this.el.playerLegendItem.style.display = '';
      // Keep maze but clear solution overlay for cleaner play view
      this.solution = null;
      if (this.maze) {
        this.el.startPlayBtn.disabled = false;
        this.render();
      }
    }
  }

  // ════════════════════════════════════════════════════════════════════════════
  // MAZE GENERATION (shared)
  // ════════════════════════════════════════════════════════════════════════════

  async generateMaze(caller) {
    const rows = caller === 'manual'
      ? parseInt(this.el.manualRowsInput.value)
      : parseInt(this.el.rowsInput.value);
    const cols = caller === 'manual'
      ? parseInt(this.el.manualColsInput.value)
      : parseInt(this.el.colsInput.value);

    // Stop any active play session
    this.stopTimer();
    this.playActive = false;
    this.playerWon  = false;

    this.setStatus('GENERATING...');

    const triggerBtn = caller === 'manual' ? this.el.manualGenerateBtn : this.el.generateBtn;
    triggerBtn.disabled = true;

    try {
      const response = await fetch('/api/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ rows, cols })
      });

      this.maze = await response.json();
      this.solution = null;

      // Reset algorithm UI
      this.el.solveBtn.disabled = false;
      this.el.submitScoreBtn.disabled = true;
      this.el.mazeSize.textContent = `${rows} × ${cols}`;
      this.el.pathLength.textContent = '-';
      this.el.solveTime.textContent = '-';

      // Reset manual UI
      this.el.startPlayBtn.disabled = false;
      this.el.giveUpBtn.disabled = true;
      this.resetPlayDisplay();

      this.resizeCanvas();
      this.render();
      this.setStatus('READY');
    } catch (error) {
      console.error('Generation failed:', error);
      this.setStatus('ERROR');
    } finally {
      triggerBtn.disabled = false;
    }
  }

  // ════════════════════════════════════════════════════════════════════════════
  // ALGORITHM SOLVER
  // ════════════════════════════════════════════════════════════════════════════

  async solveMaze() {
    if (!this.maze) return;

    const algorithm = this.el.algorithmSelect.value;
    this.setStatus('SOLVING...', true);
    this.el.solveBtn.disabled = true;

    try {
      const response = await fetch('/api/solve', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ maze: this.maze, algorithm })
      });

      const result = await response.json();

      this.solution = result.solution;
      this.solveTimeMs = result.solveTimeMs;

      this.el.pathLength.textContent = result.pathLength;
      this.el.solveTime.textContent = `${result.solveTimeMs}ms`;

      if (this.el.animatePathCheck.checked) {
        this.animateSolution();
      } else {
        this.render();
      }

      this.setStatus('SOLVED');
      this.el.submitScoreBtn.disabled = false;
    } catch (error) {
      console.error('Solving failed:', error);
      this.setStatus('ERROR');
    } finally {
      this.el.solveBtn.disabled = false;
      setTimeout(() => this.el.status.classList.remove('active'), 2000);
    }
  }

  // ════════════════════════════════════════════════════════════════════════════
  // MANUAL PLAY — CORE
  // ════════════════════════════════════════════════════════════════════════════

  startManualPlay() {
    if (!this.maze) return;

    this.stopTimer();
    this.playerPos  = { row: 0, col: 0 };
    this.steps      = 0;
    this.timerMs    = 0;
    this.playerWon  = false;
    this.visitedCells = new Set();
    this.visitedCells.add('0,0');

    this.updatePlayDisplay();
    this.el.startPlayBtn.textContent = 'RESTART';
    this.el.giveUpBtn.disabled = false;

    // Start live timer
    this.playActive = true;
    const startTime = performance.now() - this.timerMs;
    this.timerInterval = setInterval(() => {
      this.timerMs = performance.now() - startTime;
      this.updateTimerDisplay();
    }, 50);

    this.setStatus('PLAYING', 'player');
    this.render();
  }

  stopTimer() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
    this.playActive = false;
  }

  giveUp() {
    this.stopTimer();
    this.el.giveUpBtn.disabled = true;
    // Discard — show options modal (nothing saved to DB)
    this.showGiveUpModal();
  }

  handleKeyDown(e) {
    if (this.mode !== 'manual' || !this.playActive || this.playerWon) return;

    const dirMap = {
      'ArrowUp':    'top',
      'ArrowDown':  'bottom',
      'ArrowLeft':  'left',
      'ArrowRight': 'right',
    };

    const dir = dirMap[e.key];
    if (!dir) return;

    e.preventDefault();

    const cell = this.maze.cells.find(
      c => c.row === this.playerPos.row && c.col === this.playerPos.col
    );
    if (!cell) return;

    // Wall check: if that wall exists, block movement
    if (cell[dir]) return;

    // Compute new position
    const delta = { top: [-1,0], bottom: [1,0], left: [0,-1], right: [0,1] }[dir];
    const newRow = this.playerPos.row + delta[0];
    const newCol = this.playerPos.col + delta[1];

    // Bounds check
    if (newRow < 0 || newRow >= this.maze.rows || newCol < 0 || newCol >= this.maze.cols) return;

    // Move
    this.playerPos = { row: newRow, col: newCol };
    this.steps++;
    this.visitedCells.add(`${newRow},${newCol}`);

    this.updatePlayDisplay();
    this.render();

    // Win check
    if (newRow === this.maze.rows - 1 && newCol === this.maze.cols - 1) {
      this.handleWin();
    }
  }

  handleWin() {
    this.stopTimer();
    this.playerWon = true;
    this.el.giveUpBtn.disabled = true;
    this.setStatus('CLEARED!', 'win');

    // Populate win stats
    this.el.winTime.textContent  = this.formatTime(this.timerMs);
    this.el.winSteps.textContent = this.steps;

    setTimeout(() => this.showAttemptModal(), 600);
  }

  // ════════════════════════════════════════════════════════════════════════════
  // GIVE-UP MODAL ACTIONS
  // ════════════════════════════════════════════════════════════════════════════

  showGiveUpModal() {
    this.el.giveUpModal.classList.add('active');
  }

  hideGiveUpModal() {
    this.el.giveUpModal.classList.remove('active');
  }

  handleTryAgain() {
    this.hideGiveUpModal();
    this.startManualPlay();
  }

  handleTryAlgorithm() {
    this.hideGiveUpModal();
    this.switchMode('algorithm');
    // Maze is already loaded — user can hit SOLVE immediately
    this.el.solveBtn.disabled = false;
  }

  handleNewMaze() {
    this.hideGiveUpModal();
    this.generateMaze('manual');
  }

  // ════════════════════════════════════════════════════════════════════════════
  // WIN MODAL (Attempt Submit)
  // ════════════════════════════════════════════════════════════════════════════

  showAttemptModal() {
    this.el.attemptModal.classList.add('active');
    this.el.attemptPlayerName.focus();
  }

  hideAttemptModal() {
    this.el.attemptModal.classList.remove('active');
    this.el.attemptPlayerName.value = '';
    // Offer try-again options after dismissing
    this.el.startPlayBtn.textContent = 'PLAY AGAIN';
    this.el.giveUpBtn.disabled = true;
  }

  async submitAttempt() {
    const playerName = this.el.attemptPlayerName.value.trim();
    if (!playerName) {
      alert('Please enter a player name');
      return;
    }

    const mazeSize = `${this.maze.rows}x${this.maze.cols}`;

    try {
      await fetch('/api/attempts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          playerName,
          mazeSize,
          durationMs: Math.round(this.timerMs),
          steps: this.steps,
        })
      });

      this.hideAttemptModal();
      this.loadTopAttempts();
    } catch (error) {
      console.error('Failed to submit attempt:', error);
      alert('Failed to submit score');
    }
  }

  // ════════════════════════════════════════════════════════════════════════════
  // ALGORITHM SCORE MODAL
  // ════════════════════════════════════════════════════════════════════════════

  showScoreModal() {
    this.el.scoreModal.classList.add('active');
    this.el.playerNameInput.focus();
  }

  hideScoreModal() {
    this.el.scoreModal.classList.remove('active');
    this.el.playerNameInput.value = '';
  }

  async submitScore() {
    const playerName = this.el.playerNameInput.value.trim();
    if (!playerName) {
      alert('Please enter a player name');
      return;
    }

    const mazeSize  = `${this.maze.rows}x${this.maze.cols}`;
    const algorithm = this.el.algorithmSelect.value;

    try {
      await fetch('/api/scores', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ playerName, mazeSize, solveTimeMs: this.solveTimeMs, algorithm })
      });

      this.hideScoreModal();
      this.loadHighScores();
      this.el.submitScoreBtn.disabled = true;
    } catch (error) {
      console.error('Failed to submit score:', error);
      alert('Failed to submit score');
    }
  }

  // ════════════════════════════════════════════════════════════════════════════
  // LEADERBOARDS
  // ════════════════════════════════════════════════════════════════════════════

  async loadHighScores() {
    try {
      const response = await fetch('/api/scores/top?limit=5');
      const scores = await response.json();
      this.displayScores(scores);
    } catch (error) {
      console.error('Failed to load scores:', error);
    }
  }

  displayScores(scores) {
    if (scores.length === 0) {
      this.el.scoresContainer.innerHTML = '<div class="loading">NO SCORES YET</div>';
      return;
    }

    this.el.scoresContainer.innerHTML =
      `<div class="scores-header algo-header">
         <span>NAME</span><span>SIZE</span><span>TIME</span>
       </div>` +
      scores.map(s => `
        <div class="score-entry">
          <span>${s.playerName}</span>
          <span>${s.mazeSize}</span>
          <span>${s.solveTimeMs}ms</span>
        </div>
      `).join('');
  }

  async loadTopAttempts() {
    try {
      const response = await fetch('/api/attempts/top?limit=5');
      const attempts = await response.json();
      this.displayAttempts(attempts);
    } catch (error) {
      console.error('Failed to load attempts:', error);
    }
  }

  displayAttempts(attempts) {
    if (attempts.length === 0) {
      this.el.attemptsContainer.innerHTML = '<div class="loading">NO RECORDS YET</div>';
      return;
    }

    this.el.attemptsContainer.innerHTML =
      `<div class="scores-header attempts-header">
         <span>NAME</span><span>SIZE</span><span>STEPS</span><span>TIME</span>
       </div>` +
      attempts.map(a => `
        <div class="attempt-entry">
          <span>${a.playerName}</span>
          <span>${a.mazeSize}</span>
          <span class="steps-col">${a.steps}</span>
          <span>${this.formatTime(a.durationMs)}</span>
        </div>
      `).join('');
  }

  // ════════════════════════════════════════════════════════════════════════════
  // RENDERING
  // ════════════════════════════════════════════════════════════════════════════

  resizeCanvas() {
    if (!this.maze) return;

    const rows = this.maze.rows;
    const cols = this.maze.cols;
    const maxWidth  = this.canvas.parentElement.clientWidth - 100;
    const maxHeight = window.innerHeight - 300;

    const cellSizeByWidth  = Math.floor(maxWidth  / cols);
    const cellSizeByHeight = Math.floor(maxHeight / rows);

    this.cellSize = Math.min(cellSizeByWidth, cellSizeByHeight, 40);

    this.canvas.width  = cols * this.cellSize;
    this.canvas.height = rows * this.cellSize;
  }

  render() {
    if (!this.maze) return;

    this.ctx.fillStyle = '#050b14';
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

    if (this.mode === 'manual') {
      this.renderVisitedTrail();
    }

    this.renderWalls();

    if (this.solution && this.mode === 'algorithm' && this.el.showSolutionCheck.checked) {
      this.renderSolution();
    }

    this.renderMarkers();

    if (this.mode === 'manual') {
      this.renderPlayer();
    }
  }

  renderWalls() {
    this.ctx.strokeStyle = '#00d9ff';
    this.ctx.lineWidth = 2;
    this.ctx.shadowColor = 'rgba(0, 217, 255, 0.3)';
    this.ctx.shadowBlur = 4;

    for (const cellData of this.maze.cells) {
      const x = cellData.col * this.cellSize;
      const y = cellData.row * this.cellSize;

      this.ctx.beginPath();

      if (cellData.top)    { this.ctx.moveTo(x, y);                         this.ctx.lineTo(x + this.cellSize, y); }
      if (cellData.right)  { this.ctx.moveTo(x + this.cellSize, y);         this.ctx.lineTo(x + this.cellSize, y + this.cellSize); }
      if (cellData.bottom) { this.ctx.moveTo(x, y + this.cellSize);         this.ctx.lineTo(x + this.cellSize, y + this.cellSize); }
      if (cellData.left)   { this.ctx.moveTo(x, y);                         this.ctx.lineTo(x, y + this.cellSize); }

      this.ctx.stroke();
    }

    this.ctx.shadowBlur = 0;
  }

  renderVisitedTrail() {
    for (const key of this.visitedCells) {
      const [r, c] = key.split(',').map(Number);
      const x = c * this.cellSize;
      const y = r * this.cellSize;
      this.ctx.fillStyle = 'rgba(204, 51, 255, 0.13)';
      this.ctx.fillRect(x + 1, y + 1, this.cellSize - 2, this.cellSize - 2);
    }
  }

  renderSolution() {
    if (!this.solution || this.solution.length === 0) return;

    this.ctx.strokeStyle = '#ff3366';
    this.ctx.lineWidth = 3;
    this.ctx.lineCap = 'round';
    this.ctx.lineJoin = 'round';
    this.ctx.shadowColor = 'rgba(255, 51, 102, 0.6)';
    this.ctx.shadowBlur = 8;

    const drawLength = this.isAnimating ? this.animationFrame : this.solution.length;

    this.ctx.beginPath();
    for (let i = 0; i < drawLength && i < this.solution.length; i++) {
      const cell = this.solution[i];
      const x = cell.col * this.cellSize + this.cellSize / 2;
      const y = cell.row * this.cellSize + this.cellSize / 2;
      if (i === 0) this.ctx.moveTo(x, y);
      else         this.ctx.lineTo(x, y);
    }
    this.ctx.stroke();
    this.ctx.shadowBlur = 0;
  }

  renderMarkers() {
    const startX = 0 * this.cellSize + this.cellSize / 2;
    const startY = 0 * this.cellSize + this.cellSize / 2;
    const endX   = (this.maze.cols - 1) * this.cellSize + this.cellSize / 2;
    const endY   = (this.maze.rows - 1) * this.cellSize + this.cellSize / 2;

    // Start — green dot
    this.ctx.fillStyle   = '#00ff88';
    this.ctx.shadowColor = 'rgba(0, 255, 136, 0.5)';
    this.ctx.shadowBlur  = 10;
    this.ctx.beginPath();
    this.ctx.arc(startX, startY, 6, 0, Math.PI * 2);
    this.ctx.fill();

    // End — orange dot with pulsing ring when in manual mode
    this.ctx.fillStyle   = '#ffaa00';
    this.ctx.shadowColor = 'rgba(255, 170, 0, 0.5)';
    this.ctx.shadowBlur  = 10;
    this.ctx.beginPath();
    this.ctx.arc(endX, endY, 6, 0, Math.PI * 2);
    this.ctx.fill();

    this.ctx.shadowBlur = 0;
  }

  renderPlayer() {
    if (!this.playActive && !this.playerWon) return;

    const x = this.playerPos.col * this.cellSize + this.cellSize / 2;
    const y = this.playerPos.row * this.cellSize + this.cellSize / 2;
    const r = Math.max(4, this.cellSize * 0.3);

    // Outer glow ring
    this.ctx.strokeStyle = 'rgba(204, 51, 255, 0.4)';
    this.ctx.lineWidth = 2;
    this.ctx.shadowColor = 'rgba(204, 51, 255, 0.8)';
    this.ctx.shadowBlur = 16;
    this.ctx.beginPath();
    this.ctx.arc(x, y, r + 3, 0, Math.PI * 2);
    this.ctx.stroke();

    // Solid fill
    this.ctx.fillStyle  = '#cc33ff';
    this.ctx.shadowBlur = 12;
    this.ctx.beginPath();
    this.ctx.arc(x, y, r, 0, Math.PI * 2);
    this.ctx.fill();

    this.ctx.shadowBlur = 0;
  }

  animateSolution() {
    this.isAnimating = true;
    this.animationFrame = 0;

    const animate = () => {
      this.render();
      this.animationFrame++;
      if (this.animationFrame <= this.solution.length) {
        requestAnimationFrame(animate);
      } else {
        this.isAnimating = false;
      }
    };

    animate();
  }

  // ════════════════════════════════════════════════════════════════════════════
  // DISPLAY HELPERS
  // ════════════════════════════════════════════════════════════════════════════

  updatePlayDisplay() {
    this.el.playStepsEl.textContent = this.steps;
    this.updateTimerDisplay();
  }

  updateTimerDisplay() {
    this.el.playTimerEl.textContent = this.formatTime(this.timerMs);
    this.el.timer.textContent       = this.formatTime(this.timerMs);
  }

  resetPlayDisplay() {
    this.steps   = 0;
    this.timerMs = 0;
    this.el.playStepsEl.textContent = '0';
    this.el.playTimerEl.textContent = '00:00.000';
    this.el.timer.textContent       = '00:00.000';
    this.el.startPlayBtn.textContent = 'START';
    this.el.giveUpBtn.disabled = true;
    this.visitedCells = new Set();
  }

  formatTime(ms) {
    const totalSec = Math.floor(ms / 1000);
    const minutes  = Math.floor(totalSec / 60).toString().padStart(2, '0');
    const seconds  = (totalSec % 60).toString().padStart(2, '0');
    const millis   = Math.floor(ms % 1000).toString().padStart(3, '0');
    return `${minutes}:${seconds}.${millis}`;
  }

  setStatus(text, type) {
    this.el.status.textContent = text;
    this.el.status.className = 'status-item';
    if (type === true || type === 'active') this.el.status.classList.add('active');
    if (type === 'player') this.el.status.classList.add('playing');
  }
}

// ── Boot ──────────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  new MazeMaster();
});
