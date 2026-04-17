var app = angular.module('mazeApp', []);

app.service('MazeService', ['$http', function ($http) {
  this.generate = function (rows, cols) {
    return $http.post('/api/generate', { rows: rows, cols: cols });
  };
  this.solve = function (maze, algoName) {
    return $http.post('/api/solve', { maze: maze, algoName: algoName });
  };
  this.saveMaze = function (playerName, label, mazeData) {
    return $http.post('/api/mazes/save', { playerName: playerName, label: label, mazeData: mazeData });
  };
  this.loadMaze = function (id) {
    return $http.get('/api/mazes/' + id);
  };
  this.getSavedMazes = function () {
    return $http.get('/api/mazes/saved');
  };
  this.submitScore = function (payload) {
    return $http.post('/api/scores', payload);
  };
  this.getTopScores = function (limit) {
    return $http.get('/api/scores/top?limit=' + (limit || 5));
  };
  this.submitAttempt = function (payload) {
    return $http.post('/api/attempts', payload);
  };
  this.getTopAttempts = function (limit) {
    return $http.get('/api/attempts/top?limit=' + (limit || 5));
  };
}]);

app.controller('MazeController', ['$scope', '$interval', '$timeout', 'MazeService',
  function ($scope, $interval, $timeout, MazeService) {

    $scope.mode = 'algorithm';
    $scope.status = 'STANDBY';
    $scope.statusClass = {};
    $scope.maze = null;
    $scope.solution = null;
    $scope.currentMazeId = null;
    $scope.mazeSaved = false;

    $scope.algoRows = 20;
    $scope.algoCols = 20;
    $scope.selectedAlgo = 'astar';
    $scope.manualRows = 20;
    $scope.manualCols = 20;
    $scope.showSolution = true;
    $scope.animatePath = false;

    $scope.mazeSize = '-';
    $scope.pathLength = '-';
    $scope.solveTime = '-';

    $scope.topAlgoRuns = [];
    $scope.topManualRuns = [];
    $scope.savedMazes = [];

    $scope.activeModal = '';

    $scope.form = { playerName: '', attemptName: '', mazeLabel: '', saveName: '' };
    $scope.loginData = { name: '', studentId: null, preferredMode: 'algorithm' };
    $scope.loginError = '';
    $scope.student = null;

    $scope.isGenerating = false;
    $scope.isSolving = false;
    $scope.scoreSubmitted = true;

    $scope.playerPos = { row: 0, col: 0 };
    $scope.visitedCells = [];
    $scope.playActive = false;
    $scope.playerWon = false;
    $scope.playSteps = 0;
    $scope.playTimerMs = 0;
    $scope.playTimerStr = '00:00.000';

    var solveTimeMs = 0;
    var currentPathLength = 0;
    var isAnimating = false;
    var animFrame = 0;
    var timerPromise = null;

    var canvas = null;
    var ctx = null;
    var cellSize = 20;

    $timeout(function () {
      canvas = document.getElementById('mazeCanvas');
      ctx = canvas.getContext('2d');
      MazeService.getTopScores(5).then(function (r) { $scope.topAlgoRuns = r.data; });
      MazeService.getTopAttempts(5).then(function (r) { $scope.topManualRuns = r.data; });
      MazeService.getSavedMazes().then(function (r) { $scope.savedMazes = r.data; });
    }, 0);

    $scope.openModal = function (name) { $scope.activeModal = name; };
    $scope.closeModal = function () { $scope.activeModal = ''; };

    $scope.switchMode = function (newMode) {
      if ($scope.mode === 'manual' && newMode !== 'manual') stopTimer();
      $scope.mode = newMode;
      if (newMode === 'manual') $scope.solution = null;
      if ($scope.maze) $scope.render();
    };

    $scope.submitLogin = function () {
      $scope.loginError = '';

      var name = ($scope.loginData.name || '').trim();
      var id = parseInt($scope.loginData.studentId);

      if (name.length < 2) {
        $scope.loginError = 'Display name must be at least 2 characters.';
        return;
      }
      if (isNaN(id) || String(id).length !== 6) {
        $scope.loginError = 'Student ID must be exactly 6 digits (e.g. 123456).';
        return;
      }

      $scope.student = {
        name: name,
        studentId: id,
        mode: $scope.loginData.preferredMode || 'algorithm'
      };

      $scope.form.playerName = name;
      $scope.form.attemptName = name;
      $scope.form.saveName = name;
      $scope.switchMode($scope.student.mode);
      $scope.closeModal();
    };

    $scope.generateMaze = function (caller) {
      var rows = parseInt(caller === 'manual' ? $scope.manualRows : $scope.algoRows);
      var cols = parseInt(caller === 'manual' ? $scope.manualCols : $scope.algoCols);

      stopTimer();
      $scope.playActive = false;
      $scope.playerWon = false;
      $scope.currentMazeId = null;
      $scope.mazeSaved = false;
      $scope.isGenerating = true;
      setStatus('GENERATING...');

      MazeService.generate(rows, cols)
        .then(function (response) {
          $scope.maze = response.data;
          $scope.solution = null;
          $scope.scoreSubmitted = true;
          $scope.mazeSize = rows + ' × ' + cols;
          $scope.pathLength = '-';
          $scope.solveTime = '-';
          resetPlayDisplay();
          resizeCanvas();
          $scope.render();
          setStatus('READY');
        })
        .catch(function () { setStatus('ERROR'); })
        .finally(function () { $scope.isGenerating = false; });
    };

    $scope.solveMaze = function () {
      if (!$scope.maze) return;
      $scope.isSolving = true;
      setStatus('SOLVING...');

      MazeService.solve($scope.maze, $scope.selectedAlgo)
        .then(function (response) {
          $scope.solution = response.data.solution;
          solveTimeMs = response.data.solveTime;
          currentPathLength = response.data.pathLength;
          $scope.pathLength = response.data.pathLength;
          $scope.solveTime = response.data.solveTime + 'ms';
          $scope.scoreSubmitted = false;

          if ($scope.animatePath) animateSolution();
          else $scope.render();
          setStatus('SOLVED');
        })
        .catch(function () { setStatus('ERROR'); })
        .finally(function () { $scope.isSolving = false; });
    };

    $scope.canSaveMaze = function () { return !!$scope.maze && !$scope.mazeSaved; };

    $scope.saveMaze = function () {
      if (!$scope.form.saveName.trim()) { alert('Please enter your name'); return; }
      var label = $scope.form.mazeLabel.trim() ||
        ($scope.maze.rows + '×' + $scope.maze.cols + ' Maze');

      MazeService.saveMaze($scope.form.saveName.trim(), label, $scope.maze)
        .then(function (response) {
          $scope.currentMazeId = response.data.mazeId;
          $scope.mazeSaved = true;
          $scope.closeModal();
          $scope.form.saveName = '';
          $scope.form.mazeLabel = '';
          MazeService.getSavedMazes().then(function (r) { $scope.savedMazes = r.data; });
        })
        .catch(function () { alert('Failed to save maze'); });
    };

    $scope.loadMaze = function (id) {
      MazeService.loadMaze(id)
        .then(function (response) {
          $scope.maze = response.data;
          $scope.currentMazeId = id;
          $scope.solution = null;
          $scope.mazeSaved = true;
          $scope.scoreSubmitted = true;
          $scope.mazeSize = $scope.maze.rows + ' × ' + $scope.maze.cols;
          $scope.pathLength = '-';
          $scope.solveTime = '-';
          stopTimer();
          resetPlayDisplay();
          resizeCanvas();
          $scope.render();
          setStatus('LOADED');
        })
        .catch(function () { alert('Failed to load maze'); });
    };

    $scope.submitScore = function () {
      if (!$scope.form.playerName.trim()) { alert('Enter player name'); return; }
      MazeService.submitScore({
        playerName: $scope.form.playerName.trim(),
        mazeSize: $scope.maze.rows + 'x' + $scope.maze.cols,
        solveTime: solveTimeMs,
        algoName: $scope.selectedAlgo,
        pathLength: currentPathLength,
        mazeId: $scope.currentMazeId
      }).then(function () {
        $scope.scoreSubmitted = true;
        $scope.form.playerName = '';
        $scope.closeModal();
        MazeService.getTopScores(5).then(function (r) { $scope.topAlgoRuns = r.data; });
      });
    };

    $scope.submitAttempt = function () {
      if (!$scope.form.attemptName.trim()) { alert('Enter player name'); return; }
      MazeService.submitAttempt({
        playerName: $scope.form.attemptName.trim(),
        mazeSize: $scope.maze.rows + 'x' + $scope.maze.cols,
        duration: Math.round($scope.playTimerMs),
        steps: $scope.playSteps,
        mazeId: $scope.currentMazeId
      }).then(function () {
        $scope.form.attemptName = '';
        $scope.closeModal();
        MazeService.getTopAttempts(5).then(function (r) { $scope.topManualRuns = r.data; });
      });
    };

    $scope.startManualPlay = function () {
      if (!$scope.maze) return;
      stopTimer();
      $scope.playerPos = { row: 0, col: 0 };
      $scope.visitedCells = ['0,0'];
      $scope.playSteps = 0;
      $scope.playTimerMs = 0;
      $scope.playerWon = false;
      $scope.playActive = true;
      updateTimerDisplay();

      var t0 = performance.now();
      timerPromise = $interval(function () {
        $scope.playTimerMs = performance.now() - t0;
        updateTimerDisplay();
      }, 50);

      setStatus('PLAYING');
      $scope.render();
    };

    $scope.giveUp = function () { stopTimer(); $scope.openModal('giveUp'); };
    $scope.tryAgain = function () { $scope.closeModal(); $scope.startManualPlay(); };
    $scope.tryAlgorithm = function () { $scope.closeModal(); $scope.switchMode('algorithm'); };
    $scope.newMaze = function () { $scope.closeModal(); $scope.generateMaze('manual'); };

    function handleWin() {
      stopTimer();
      $scope.playerWon = true;
      setStatus('CLEARED!');
      $timeout(function () { $scope.openModal('attempt'); }, 600);
    }

    function stopTimer() {
      if (timerPromise) { $interval.cancel(timerPromise); timerPromise = null; }
      $scope.playActive = false;
    }

    $scope.formatTime = function (ms) {
      ms = ms || 0;
      var totalSec = Math.floor(ms / 1000);
      var mm = Math.floor(totalSec / 60).toString().padStart(2, '0');
      var ss = (totalSec % 60).toString().padStart(2, '0');
      var mmm = Math.floor(ms % 1000).toString().padStart(3, '0');
      return mm + ':' + ss + '.' + mmm;
    };

    function updateTimerDisplay() {
      $scope.playTimerStr = $scope.formatTime($scope.playTimerMs);
    }

    function resetPlayDisplay() {
      $scope.playSteps = 0;
      $scope.playTimerMs = 0;
      $scope.visitedCells = [];
      updateTimerDisplay();
    }

    function setStatus(text) {
      $scope.status = text;
      $scope.statusClass = {
        'active': text === 'SOLVING...',
        'playing': text === 'PLAYING'
      };
    }

    function resizeCanvas() {
      if (!$scope.maze || !canvas) return;
      var maxW = canvas.parentElement.clientWidth - 100;
      var maxH = window.innerHeight - 300;
      cellSize = Math.min(
        Math.floor(maxW / $scope.maze.cols),
        Math.floor(maxH / $scope.maze.rows),
        40
      );
      canvas.width = $scope.maze.cols * cellSize;
      canvas.height = $scope.maze.rows * cellSize;
    }

    $scope.render = function () {
      if (!$scope.maze || !ctx) return;
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      if ($scope.mode === 'manual') renderTrail();
      renderWalls();
      if ($scope.solution && $scope.mode === 'algorithm' && $scope.showSolution)
        renderSolution($scope.solution.length);
      renderMarkers();
      if ($scope.mode === 'manual') renderPlayer();
    };

    function renderWalls() {
      ctx.strokeStyle = '#000000';
      ctx.lineWidth = 2;
      $scope.maze.cells.forEach(function (cd) {
        var x = cd.col * cellSize, y = cd.row * cellSize;
        ctx.beginPath();
        if (cd.top) { ctx.moveTo(x, y); ctx.lineTo(x + cellSize, y); }
        if (cd.right) { ctx.moveTo(x + cellSize, y); ctx.lineTo(x + cellSize, y + cellSize); }
        if (cd.bottom) { ctx.moveTo(x, y + cellSize); ctx.lineTo(x + cellSize, y + cellSize); }
        if (cd.left) { ctx.moveTo(x, y); ctx.lineTo(x, y + cellSize); }
        ctx.stroke();
      });
    }

    function renderTrail() {
      if (!$scope.visitedCells || !$scope.visitedCells.length) return;
      ctx.strokeStyle = '#6366f1';
      ctx.lineWidth = 3;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      ctx.beginPath();
      $scope.visitedCells.forEach(function (key, i) {
        var parts = key.split(',');
        var r = parseInt(parts[0]);
        var c = parseInt(parts[1]);
        var x = c * cellSize + cellSize / 2;
        var y = r * cellSize + cellSize / 2;
        if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
      });
      ctx.stroke();
    }

    function renderSolution(upToIndex) {
      if (!$scope.solution || !$scope.solution.length) return;
      ctx.strokeStyle = '#6366f1';
      ctx.lineWidth = 3;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      ctx.beginPath();
      for (var i = 0; i < upToIndex && i < $scope.solution.length; i++) {
        var x = $scope.solution[i].col * cellSize + cellSize / 2;
        var y = $scope.solution[i].row * cellSize + cellSize / 2;
        if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
      }
      ctx.stroke();
    }

    function renderMarkers() {
      var sx = cellSize / 2, sy = cellSize / 2;
      var ex = ($scope.maze.cols - 1) * cellSize + cellSize / 2;
      var ey = ($scope.maze.rows - 1) * cellSize + cellSize / 2;

      ctx.fillStyle = '#ffffff';
      ctx.strokeStyle = '#000000';
      ctx.lineWidth = 2;
      ctx.beginPath(); ctx.arc(sx, sy, 6, 0, Math.PI * 2); ctx.fill(); ctx.stroke();

      ctx.fillStyle = '#333333';
      ctx.beginPath(); ctx.arc(ex, ey, 6, 0, Math.PI * 2); ctx.fill();
    }

    function renderPlayer() {
      if (!$scope.playActive && !$scope.playerWon) return;
      var x = $scope.playerPos.col * cellSize + cellSize / 2;
      var y = $scope.playerPos.row * cellSize + cellSize / 2;
      var r = Math.max(4, cellSize * 0.3);

      ctx.fillStyle = '#666666';
      ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2); ctx.fill();
    }

    function animateSolution() {
      isAnimating = true;
      animFrame = 0;
      function step() {
        $scope.render();
        renderSolution(animFrame);
        animFrame++;
        if (animFrame <= $scope.solution.length) requestAnimationFrame(step);
        else isAnimating = false;
      }
      step();
    }

    $scope.movePlayer = function (dir) {
      if (!$scope.playActive || $scope.playerWon || !$scope.maze) return;

      var cell = $scope.maze.cells.find(function (c) {
        return c.row === $scope.playerPos.row && c.col === $scope.playerPos.col;
      });
      if (!cell || cell[dir]) return;

      var delta = { top: [-1, 0], bottom: [1, 0], left: [0, -1], right: [0, 1] };
      var newRow = $scope.playerPos.row + delta[dir][0];
      var newCol = $scope.playerPos.col + delta[dir][1];
      if (newRow < 0 || newRow >= $scope.maze.rows) return;
      if (newCol < 0 || newCol >= $scope.maze.cols) return;

      $scope.playerPos = { row: newRow, col: newCol };
      $scope.playSteps++;
      $scope.visitedCells.push(newRow + ',' + newCol);
      $scope.render();

      if (newRow === $scope.maze.rows - 1 && newCol === $scope.maze.cols - 1) {
        handleWin();
      }
    };

  }]);

app.directive('mzKeyHandler', function () {
  return {
    restrict: 'A',
    link: function (scope) {
      var keyMap = {
        ArrowUp: 'top', ArrowDown: 'bottom',
        ArrowLeft: 'left', ArrowRight: 'right'
      };
      document.addEventListener('keydown', function (e) {
        var dir = keyMap[e.key];
        if (!dir || scope.mode !== 'manual') return;
        e.preventDefault();
        scope.$apply(function () { scope.movePlayer(dir); });
      });
    }
  };
});
