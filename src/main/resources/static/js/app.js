class MazeMaster {
  constructor() {
    this.canvas = document.getElementById('mazeCanvas');
    this.ctx    = this.canvas.getContext('2d');

    // ── Shared state ──────────────────────────────────────────────────────
    this.maze         = null;
    this.solution     = null;
    this.cellSize     = 20;
    this.animationFrame = 0;
    this.isAnimating  = false;
    this.solveTimeMs  = 0;
    this.currentPathLength = 0;

    // ── Mode ─────────────────────────────────────────────────────────────
    this.mode = 'algorithm';

    // ── Saved maze reference ──────────────────────────────────────────────
    this.currentMazeId = null;   // non-null when maze is saved or loaded

    // ── Manual play state ─────────────────────────────────────────────────
    this.playerPos     = { row: 0, col: 0 };
    this.steps         = 0;
    this.timerMs       = 0;
    this.timerInterval = null;
    this.playActive    = false;
    this.playerWon     = false;
    this.visitedCells  = new Set();

    this.initializeElements();
    this.bindEvents();
    this.loadHighScores();
    this.loadTopAttempts();
    this.loadSavedMazes();
  }

  // ══════════════════════════════════════════════════════════════════════════
  // INIT
  // ══════════════════════════════════════════════════════════════════════════

  initializeElements() {
    this.el = {
      // Mode toggles
      tabAlgorithm:      document.getElementById('tabAlgorithm'),
      tabManual:         document.getElementById('tabManual'),
      algorithmPanel:    document.getElementById('algorithmPanel'),
      manualPanel:       document.getElementById('manualPanel'),

      // Algorithm controls
      generateBtn:       document.getElementById('generateBtn'),
      solveBtn:          document.getElementById('solveBtn'),
      submitScoreBtn:    document.getElementById('submitScoreBtn'),
      saveMazeBtn:       document.getElementById('saveMazeBtn'),
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
      scoresContainer:   document.getElementById('scoresContainer'),

      // Manual controls
      manualGenerateBtn:   document.getElementById('manualGenerateBtn'),
      saveMazeBtnManual:   document.getElementById('saveMazeBtnManual'),
      manualRowsInput:     document.getElementById('manualRows'),
      manualColsInput:     document.getElementById('manualCols'),
      startPlayBtn:        document.getElementById('startPlayBtn'),
      giveUpBtn:           document.getElementById('giveUpBtn'),
      playTimerEl:         document.getElementById('playTimer'),
      playStepsEl:         document.getElementById('playSteps'),
      attemptsContainer:   document.getElementById('attemptsContainer'),
      playerLegendItem:    document.getElementById('playerLegendItem'),

      // Saved mazes containers
      savedMazesContainerAlgo:   document.getElementById('savedMazesContainerAlgo'),
      savedMazesContainerManual: document.getElementById('savedMazesContainerManual'),

      // Algorithm score modal
      scoreModal:        document.getElementById('scoreModal'),
      playerNameInput:   document.getElementById('playerName'),
      confirmScoreBtn:   document.getElementById('confirmScore'),
      cancelScoreBtn:    document.getElementById('cancelScore'),

      // Manual win modal
      attemptModal:         document.getElementById('attemptModal'),
      winTime:              document.getElementById('winTime'),
      winSteps:             document.getElementById('winSteps'),
      attemptPlayerName:    document.getElementById('attemptPlayerName'),
      confirmAttemptBtn:    document.getElementById('confirmAttempt'),
      cancelAttemptBtn:     document.getElementById('cancelAttempt'),

      // Give-up modal
      giveUpModal:       document.getElementById('giveUpModal'),
      tryAgainBtn:       document.getElementById('tryAgainBtn'),
      tryAlgorithmBtn:   document.getElementById('tryAlgorithmBtn'),
      newMazeBtn:        document.getElementById('newMazeBtn'),

      // Save maze modal
      saveMazeModal:         document.getElementById('saveMazeModal'),
      mazeLabelInput:        document.getElementById('mazeLabelInput'),
      saveMazePlayerName:    document.getElementById('saveMazePlayerName'),
      confirmSaveMazeBtn:    document.getElementById('confirmSaveMaze'),
      cancelSaveMazeBtn:     document.getElementById('cancelSaveMaze'),
    };
  }

  bindEvents() {
    // Mode tabs
    this.el.tabAlgorithm.addEventListener('click', () => this.switchMode('algorithm'));
    this.el.tabManual.addEventListener('click',    () => this.switchMode('manual'));

    // Algorithm panel
    this.el.generateBtn.addEventListener('click',       () => this.generateMaze('algorithm'));
    this.el.saveMazeBtn.addEventListener('click',       () => this.showSaveMazeModal());
    this.el.solveBtn.addEventListener('click',          () => this.solveMaze());
    this.el.submitScoreBtn.addEventListener('click',    () => this.showScoreModal());
    this.el.confirmScoreBtn.addEventListener('click',   () => this.submitScore());
    this.el.cancelScoreBtn.addEventListener('click',    () => this.hideScoreModal());
    this.el.showSolutionCheck.addEventListener('change',() => this.render());
    this.el.animatePathCheck.addEventListener('change', () => { if (this.solution) this.render(); });

    // Manual panel
    this.el.manualGenerateBtn.addEventListener('click', () => this.generateMaze('manual'));
    this.el.saveMazeBtnManual.addEventListener('click', () => this.showSaveMazeModal());
    this.el.startPlayBtn.addEventListener('click',      () => this.startManualPlay());
    this.el.giveUpBtn.addEventListener('click',         () => this.giveUp());

    // Win modal
    this.el.confirmAttemptBtn.addEventListener('click', () => this.submitAttempt());
    this.el.cancelAttemptBtn.addEventListener('click',  () => this.hideAttemptModal());

    // Give-up modal
    this.el.tryAgainBtn.addEventListener('click',      () => this.handleTryAgain());
    this.el.tryAlgorithmBtn.addEventListener('click',  () => this.handleTryAlgorithm());
    this.el.newMazeBtn.addEventListener('click',       () => this.handleNewMaze());

    // Save maze modal
    this.el.confirmSaveMazeBtn.addEventListener('click', () => this.saveMaze());
    this.el.cancelSaveMazeBtn.addEventListener('click',  () => this.hideSaveMazeModal());

    // Arrow keys for manual play
    document.addEventListener('keydown', (e) => this.handleKeyDown(e));
  }

  // ══════════════════════════════════════════════════════════════════════════
  // MODE SWITCHING
  // ══════════════════════════════════════════════════════════════════════════

  switchMode(mode) {
    if (this.mode === 'manual' && mode !== 'manual') this.stopTimer();
    this.mode = mode;

    if (mode === 'algorithm') {
      this.el.tabAlgorithm.classList.add('active');
      this.el.tabManual.classList.remove('active');
      this.el.algorithmPanel.classList.remove('hidden');
      this.el.manualPanel.classList.add('hidden');
      this.el.playerLegendItem.style.display = 'none';
      if (this.maze) this.render();
    } else {
      this.el.tabManual.classList.add('active');
      this.el.tabAlgorithm.classList.remove('active');
      this.el.manualPanel.classList.remove('hidden');
      this.el.algorithmPanel.classList.add('hidden');
      this.el.playerLegendItem.style.display = '';
      this.solution = null;
      if (this.maze) {
        this.el.startPlayBtn.disabled = false;
        this.render();
      }
    }
  }

  // ══════════════════════════════════════════════════════════════════════════
  // MAZE GENERATION
  // ══════════════════════════════════════════════════════════════════════════

  async generateMaze(caller) {
    const rows = caller === 'manual'
      ? parseInt(this.el.manualRowsInput.value)
      : parseInt(this.el.rowsInput.value);
    const cols = caller === 'manual'
      ? parseInt(this.el.manualColsInput.value)
      : parseInt(this.el.colsInput.value);

    this.stopTimer();
    this.playActive   = false;
    this.playerWon    = false;
    this.currentMazeId = null;
    this.setSaveButtonState('unsaved');

    this.setStatus('GENERATING...');
    const btn = caller === 'manual' ? this.el.manualGenerateBtn : this.el.generateBtn;
    btn.disabled = true;

    try {
      const res  = await fetch('/api/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ rows, cols })
      });
      this.maze     = await res.json();
      this.solution = null;

      this.el.solveBtn.disabled      = false;
      this.el.submitScoreBtn.disabled = true;
      this.el.saveMazeBtn.disabled   = false;
      this.el.saveMazeBtnManual.disabled = false;
      this.el.startPlayBtn.disabled  = false;
      this.el.giveUpBtn.disabled     = true;

      this.el.mazeSize.textContent   = `${rows} × ${cols}`;
      this.el.pathLength.textContent = '-';
      this.el.solveTime.textContent  = '-';
      this.resetPlayDisplay();

      this.resizeCanvas();
      this.render();
      this.setStatus('READY');
    } catch (err) {
      console.error('Generation failed:', err);
      this.setStatus('ERROR');
    } finally {
      btn.disabled = false;
    }
  }

  // ══════════════════════════════════════════════════════════════════════════
  // ALGORITHM SOLVER
  // ══════════════════════════════════════════════════════════════════════════

  async solveMaze() {
    if (!this.maze) return;
    const algorithm = this.el.algorithmSelect.value;
    this.setStatus('SOLVING...', 'active');
    this.el.solveBtn.disabled = true;

    try {
      const res    = await fetch('/api/solve', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ maze: this.maze, algoName: algorithm })
      });
      const result = await res.json();

      this.solution          = result.solution;
      this.solveTimeMs       = result.solveTime;
      this.currentPathLength = result.pathLength;

      this.el.pathLength.textContent    = result.pathLength;
      this.el.solveTime.textContent     = `${result.solveTime}ms`;
      this.el.submitScoreBtn.disabled   = false;

      if (this.el.animatePathCheck.checked) this.animateSolution();
      else this.render();

      this.setStatus('SOLVED');
    } catch (err) {
      console.error('Solve failed:', err);
      this.setStatus('ERROR');
    } finally {
      this.el.solveBtn.disabled = false;
      setTimeout(() => this.el.status.classList.remove('active'), 2000);
    }
  }

  // ══════════════════════════════════════════════════════════════════════════
  // MANUAL PLAY — CORE
  // ══════════════════════════════════════════════════════════════════════════

  startManualPlay() {
    if (!this.maze) return;
    this.stopTimer();
    this.playerPos    = { row: 0, col: 0 };
    this.steps        = 0;
    this.timerMs      = 0;
    this.playerWon    = false;
    this.visitedCells = new Set(['0,0']);

    this.updatePlayDisplay();
    this.el.startPlayBtn.textContent = 'RESTART';
    this.el.giveUpBtn.disabled       = false;

    this.playActive = true;
    const t0 = performance.now();
    this.timerInterval = setInterval(() => {
      this.timerMs = performance.now() - t0;
      this.updateTimerDisplay();
    }, 50);

    this.setStatus('PLAYING', 'player');
    this.render();
  }

  stopTimer() {
    if (this.timerInterval) { clearInterval(this.timerInterval); this.timerInterval = null; }
    this.playActive = false;
  }

  giveUp() {
    this.stopTimer();
    this.el.giveUpBtn.disabled = true;
    this.showGiveUpModal();
  }

  handleKeyDown(e) {
    if (this.mode !== 'manual' || !this.playActive || this.playerWon) return;
    const dirMap = { ArrowUp:'top', ArrowDown:'bottom', ArrowLeft:'left', ArrowRight:'right' };
    const dir = dirMap[e.key];
    if (!dir) return;
    e.preventDefault();

    const cell = this.maze.cells.find(c => c.row === this.playerPos.row && c.col === this.playerPos.col);
    if (!cell || cell[dir]) return;

    const delta = { top:[-1,0], bottom:[1,0], left:[0,-1], right:[0,1] }[dir];
    const newRow = this.playerPos.row + delta[0];
    const newCol = this.playerPos.col + delta[1];
    if (newRow < 0 || newRow >= this.maze.rows || newCol < 0 || newCol >= this.maze.cols) return;

    this.playerPos = { row: newRow, col: newCol };
    this.steps++;
    this.visitedCells.add(`${newRow},${newCol}`);
    this.updatePlayDisplay();
    this.render();

    if (newRow === this.maze.rows - 1 && newCol === this.maze.cols - 1) this.handleWin();
  }

  handleWin() {
    this.stopTimer();
    this.playerWon            = true;
    this.el.giveUpBtn.disabled = true;
    this.setStatus('CLEARED!', 'win');
    this.el.winTime.textContent  = this.formatTime(this.timerMs);
    this.el.winSteps.textContent = this.steps;
    setTimeout(() => this.showAttemptModal(), 600);
  }

  // ══════════════════════════════════════════════════════════════════════════
  // SAVE MAZE
  // ══════════════════════════════════════════════════════════════════════════

  showSaveMazeModal() {
    if (!this.maze) return;
    this.el.saveMazeModal.classList.add('active');
    this.el.mazeLabelInput.focus();
  }

  hideSaveMazeModal() {
    this.el.saveMazeModal.classList.remove('active');
    this.el.mazeLabelInput.value     = '';
    this.el.saveMazePlayerName.value = '';
  }

  async saveMaze() {
    const playerName = this.el.saveMazePlayerName.value.trim();
    const label      = this.el.mazeLabelInput.value.trim()
                       || `${this.maze.rows}×${this.maze.cols} Maze`;

    if (!playerName) { alert('Please enter your name'); return; }

    try {
      const res    = await fetch('/api/mazes/save', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ playerName, label, mazeData: this.maze })
      });
      const result = await res.json();
      this.currentMazeId = result.mazeId;

      this.hideSaveMazeModal();
      this.setSaveButtonState('saved');
      this.loadSavedMazes();
    } catch (err) {
      console.error('Save failed:', err);
      alert('Failed to save maze');
    }
  }

  setSaveButtonState(state) {
    const text = state === 'saved' ? 'SAVED ✓' : 'SAVE MAZE';
    const disabled = state !== 'ready';
    [this.el.saveMazeBtn, this.el.saveMazeBtnManual].forEach(btn => {
      btn.textContent = text;
      // re-enable both save buttons only when maze exists and not yet saved
      if (state === 'ready')   { btn.disabled = false; btn.textContent = 'SAVE MAZE'; }
      if (state === 'saved')   { btn.disabled = true;  btn.textContent = 'SAVED ✓'; }
      if (state === 'unsaved') { btn.disabled = true;  btn.textContent = 'SAVE MAZE'; }
    });
  }

  // ══════════════════════════════════════════════════════════════════════════
  // LOAD SAVED MAZES
  // ══════════════════════════════════════════════════════════════════════════

  async loadSavedMazes() {
    try {
      const res   = await fetch('/api/mazes/saved');
      const mazes = await res.json();
      this.displaySavedMazes(mazes);
    } catch (err) {
      console.error('Failed to load saved mazes:', err);
    }
  }

  displaySavedMazes(mazes) {
    if (mazes.length === 0) {
      const empty = '<div class="loading">NO SAVED MAZES</div>';
      this.el.savedMazesContainerAlgo.innerHTML   = empty;
      this.el.savedMazesContainerManual.innerHTML = empty;
      return;
    }

    const html =
      `<div class="scores-header saved-header">
         <span>LABEL</span><span>SIZE</span><span>BY</span><span></span>
       </div>` +
      mazes.map(m => `
        <div class="saved-maze-entry">
          <span class="maze-label-col" title="${m.label}">${m.label}</span>
          <span>${m.rows}×${m.cols}</span>
          <span class="maze-player-col">${m.playerName}</span>
          <button class="btn-load" onclick="window.mazeApp.loadMaze(${m.id})">LOAD</button>
        </div>
      `).join('');

    this.el.savedMazesContainerAlgo.innerHTML   = html;
    this.el.savedMazesContainerManual.innerHTML = html;
  }

  async loadMaze(id) {
    try {
      const res = await fetch(`/api/mazes/${id}`);
      if (!res.ok) throw new Error('Not found');

      this.maze          = await res.json();
      this.currentMazeId = id;
      this.solution      = null;

      this.stopTimer();
      this.resetPlayDisplay();
      this.playerWon = false;

      this.el.solveBtn.disabled        = false;
      this.el.startPlayBtn.disabled    = false;
      this.el.submitScoreBtn.disabled  = true;
      this.setSaveButtonState('saved'); // already in DB

      this.el.mazeSize.textContent   = `${this.maze.rows} × ${this.maze.cols}`;
      this.el.pathLength.textContent = '-';
      this.el.solveTime.textContent  = '-';

      this.resizeCanvas();
      this.render();
      this.setStatus('LOADED');
    } catch (err) {
      console.error('Load failed:', err);
      alert('Failed to load maze');
    }
  }

  // ══════════════════════════════════════════════════════════════════════════
  // GIVE-UP MODAL
  // ══════════════════════════════════════════════════════════════════════════

  showGiveUpModal()  { this.el.giveUpModal.classList.add('active'); }
  hideGiveUpModal()  { this.el.giveUpModal.classList.remove('active'); }
  handleTryAgain()   { this.hideGiveUpModal(); this.startManualPlay(); }
  handleTryAlgorithm() {
    this.hideGiveUpModal();
    this.switchMode('algorithm');
    this.el.solveBtn.disabled = false;
  }
  handleNewMaze() { this.hideGiveUpModal(); this.generateMaze('manual'); }

  // ══════════════════════════════════════════════════════════════════════════
  // WIN MODAL (Manual Attempt Submit)
  // ══════════════════════════════════════════════════════════════════════════

  showAttemptModal() {
    this.el.attemptModal.classList.add('active');
    this.el.attemptPlayerName.focus();
  }

  hideAttemptModal() {
    this.el.attemptModal.classList.remove('active');
    this.el.attemptPlayerName.value  = '';
    this.el.startPlayBtn.textContent = 'PLAY AGAIN';
    this.el.giveUpBtn.disabled       = true;
  }

  async submitAttempt() {
    const playerName = this.el.attemptPlayerName.value.trim();
    if (!playerName) { alert('Please enter a player name'); return; }

    const mazeSize = `${this.maze.rows}x${this.maze.cols}`;
    try {
      await fetch('/api/attempts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          playerName,
          mazeSize,
          duration: Math.round(this.timerMs),
          steps:    this.steps,
          mazeId:   this.currentMazeId
        })
      });
      this.hideAttemptModal();
      this.loadTopAttempts();
    } catch (err) {
      console.error('Submit attempt failed:', err);
      alert('Failed to submit score');
    }
  }

  // ══════════════════════════════════════════════════════════════════════════
  // ALGORITHM SCORE MODAL
  // ══════════════════════════════════════════════════════════════════════════

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
    if (!playerName) { alert('Please enter a player name'); return; }

    const mazeSize  = `${this.maze.rows}x${this.maze.cols}`;
    const algorithm = this.el.algorithmSelect.value;
    try {
      await fetch('/api/scores', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          playerName,
          mazeSize,
          solveTime:  this.solveTimeMs,
          algoName:   algorithm,
          pathLength: this.currentPathLength,
          mazeId:     this.currentMazeId
        })
      });
      this.hideScoreModal();
      this.loadHighScores();
      this.el.submitScoreBtn.disabled = true;
    } catch (err) {
      console.error('Submit score failed:', err);
      alert('Failed to submit score');
    }
  }

  // ══════════════════════════════════════════════════════════════════════════
  // LEADERBOARDS
  // ══════════════════════════════════════════════════════════════════════════

  async loadHighScores() {
    try {
      const res    = await fetch('/api/scores/top?limit=5');
      const scores = await res.json();
      this.displayScores(scores);
    } catch (err) { console.error('Failed to load scores:', err); }
  }

  displayScores(scores) {
    if (scores.length === 0) {
      this.el.scoresContainer.innerHTML = '<div class="loading">NO SCORES YET</div>';
      return;
    }
    this.el.scoresContainer.innerHTML =
      `<div class="scores-header algo-header"><span>NAME</span><span>SIZE</span><span>ALGO</span><span>TIME</span></div>` +
      scores.map(s => `
        <div class="score-entry" style="grid-template-columns:1fr 1fr 1fr 1fr">
          <span>${s.playerName}</span>
          <span>${s.mazeSize}</span>
          <span>${s.algoName.toUpperCase()}</span>
          <span>${s.solveTime}ms</span>
        </div>
      `).join('');
  }

  async loadTopAttempts() {
    try {
      const res      = await fetch('/api/attempts/top?limit=5');
      const attempts = await res.json();
      this.displayAttempts(attempts);
    } catch (err) { console.error('Failed to load attempts:', err); }
  }

  displayAttempts(attempts) {
    if (attempts.length === 0) {
      this.el.attemptsContainer.innerHTML = '<div class="loading">NO RECORDS YET</div>';
      return;
    }
    this.el.attemptsContainer.innerHTML =
      `<div class="scores-header attempts-header"><span>NAME</span><span>SIZE</span><span>STEPS</span><span>TIME</span></div>` +
      attempts.map(a => `
        <div class="attempt-entry">
          <span>${a.playerName}</span>
          <span>${a.mazeSize}</span>
          <span class="steps-col">${a.steps}</span>
          <span>${this.formatTime(a.duration)}</span>
        </div>
      `).join('');
  }

  // ══════════════════════════════════════════════════════════════════════════
  // RENDERING
  // ══════════════════════════════════════════════════════════════════════════

  resizeCanvas() {
    if (!this.maze) return;
    const maxW = this.canvas.parentElement.clientWidth - 100;
    const maxH = window.innerHeight - 300;
    this.cellSize = Math.min(
      Math.floor(maxW / this.maze.cols),
      Math.floor(maxH / this.maze.rows),
      40
    );
    this.canvas.width  = this.maze.cols * this.cellSize;
    this.canvas.height = this.maze.rows * this.cellSize;
  }

  render() {
    if (!this.maze) return;
    this.ctx.fillStyle = '#050b14';
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

    if (this.mode === 'manual') this.renderVisitedTrail();
    this.renderWalls();
    if (this.solution && this.mode === 'algorithm' && this.el.showSolutionCheck.checked)
      this.renderSolution();
    this.renderMarkers();
    if (this.mode === 'manual') this.renderPlayer();
  }

  renderWalls() {
    this.ctx.strokeStyle = '#00d9ff';
    this.ctx.lineWidth   = 2;
    this.ctx.shadowColor = 'rgba(0, 217, 255, 0.3)';
    this.ctx.shadowBlur  = 4;
    for (const cd of this.maze.cells) {
      const x = cd.col * this.cellSize, y = cd.row * this.cellSize;
      this.ctx.beginPath();
      if (cd.top)    { this.ctx.moveTo(x, y);                        this.ctx.lineTo(x + this.cellSize, y); }
      if (cd.right)  { this.ctx.moveTo(x + this.cellSize, y);        this.ctx.lineTo(x + this.cellSize, y + this.cellSize); }
      if (cd.bottom) { this.ctx.moveTo(x, y + this.cellSize);        this.ctx.lineTo(x + this.cellSize, y + this.cellSize); }
      if (cd.left)   { this.ctx.moveTo(x, y);                        this.ctx.lineTo(x, y + this.cellSize); }
      this.ctx.stroke();
    }
    this.ctx.shadowBlur = 0;
  }

  renderVisitedTrail() {
    for (const key of this.visitedCells) {
      const [r, c] = key.split(',').map(Number);
      this.ctx.fillStyle = 'rgba(204, 51, 255, 0.13)';
      this.ctx.fillRect(c * this.cellSize + 1, r * this.cellSize + 1, this.cellSize - 2, this.cellSize - 2);
    }
  }

  renderSolution() {
    if (!this.solution || !this.solution.length) return;
    this.ctx.strokeStyle = '#ff3366';
    this.ctx.lineWidth   = 3;
    this.ctx.lineCap     = 'round';
    this.ctx.lineJoin    = 'round';
    this.ctx.shadowColor = 'rgba(255, 51, 102, 0.6)';
    this.ctx.shadowBlur  = 8;
    const len = this.isAnimating ? this.animationFrame : this.solution.length;
    this.ctx.beginPath();
    for (let i = 0; i < len && i < this.solution.length; i++) {
      const { col, row } = this.solution[i];
      const x = col * this.cellSize + this.cellSize / 2;
      const y = row * this.cellSize + this.cellSize / 2;
      i === 0 ? this.ctx.moveTo(x, y) : this.ctx.lineTo(x, y);
    }
    this.ctx.stroke();
    this.ctx.shadowBlur = 0;
  }

  renderMarkers() {
    const sx = this.cellSize / 2, sy = this.cellSize / 2;
    const ex = (this.maze.cols - 1) * this.cellSize + this.cellSize / 2;
    const ey = (this.maze.rows - 1) * this.cellSize + this.cellSize / 2;

    this.ctx.fillStyle   = '#00ff88';
    this.ctx.shadowColor = 'rgba(0,255,136,0.5)';
    this.ctx.shadowBlur  = 10;
    this.ctx.beginPath(); this.ctx.arc(sx, sy, 6, 0, Math.PI * 2); this.ctx.fill();

    this.ctx.fillStyle   = '#ffaa00';
    this.ctx.shadowColor = 'rgba(255,170,0,0.5)';
    this.ctx.beginPath(); this.ctx.arc(ex, ey, 6, 0, Math.PI * 2); this.ctx.fill();
    this.ctx.shadowBlur = 0;
  }

  renderPlayer() {
    if (!this.playActive && !this.playerWon) return;
    const x = this.playerPos.col * this.cellSize + this.cellSize / 2;
    const y = this.playerPos.row * this.cellSize + this.cellSize / 2;
    const r = Math.max(4, this.cellSize * 0.3);

    this.ctx.strokeStyle = 'rgba(204,51,255,0.4)';
    this.ctx.lineWidth   = 2;
    this.ctx.shadowColor = 'rgba(204,51,255,0.8)';
    this.ctx.shadowBlur  = 16;
    this.ctx.beginPath(); this.ctx.arc(x, y, r + 3, 0, Math.PI * 2); this.ctx.stroke();

    this.ctx.fillStyle  = '#cc33ff';
    this.ctx.shadowBlur = 12;
    this.ctx.beginPath(); this.ctx.arc(x, y, r, 0, Math.PI * 2); this.ctx.fill();
    this.ctx.shadowBlur = 0;
  }

  animateSolution() {
    this.isAnimating    = true;
    this.animationFrame = 0;
    const animate = () => {
      this.render();
      this.animationFrame++;
      if (this.animationFrame <= this.solution.length) requestAnimationFrame(animate);
      else this.isAnimating = false;
    };
    animate();
  }

  // ══════════════════════════════════════════════════════════════════════════
  // DISPLAY HELPERS
  // ══════════════════════════════════════════════════════════════════════════

  updatePlayDisplay() {
    this.el.playStepsEl.textContent = this.steps;
    this.updateTimerDisplay();
  }

  updateTimerDisplay() {
    const fmt = this.formatTime(this.timerMs);
    this.el.playTimerEl.textContent = fmt;
    this.el.timer.textContent       = fmt;
  }

  resetPlayDisplay() {
    this.steps   = 0;
    this.timerMs = 0;
    this.el.playStepsEl.textContent = '0';
    this.el.playTimerEl.textContent = '00:00.000';
    this.el.timer.textContent       = '00:00.000';
    this.el.startPlayBtn.textContent = 'START';
    this.el.giveUpBtn.disabled       = true;
    this.visitedCells = new Set();
  }

  formatTime(ms) {
    const totalSec = Math.floor(ms / 1000);
    const mm  = Math.floor(totalSec / 60).toString().padStart(2, '0');
    const ss  = (totalSec % 60).toString().padStart(2, '0');
    const mmm = Math.floor(ms % 1000).toString().padStart(3, '0');
    return `${mm}:${ss}.${mmm}`;
  }

  setStatus(text, type) {
    this.el.status.textContent = text;
    this.el.status.className   = 'status-item';
    if (type === 'active')  this.el.status.classList.add('active');
    if (type === 'player')  this.el.status.classList.add('playing');
  }
}

// ── Boot ──────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  window.mazeApp = new MazeMaster();
});
