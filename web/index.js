const fs = require('fs-extra');
const prettyBytes = require('pretty-bytes');
const moment = require('moment');

const sourceDir = process.argv[2];
const indexFile = 'index.html';
const indexTpl = indexFile + '.tpl';
const styleFile = 'style.css';
const listFile = 'list.txt';

const clockSince = moment('2017-04');

function numberFormat(n) {
  return new Intl.NumberFormat().format(n);
}

function fileInfo(gameCounts, n) {
  const path = sourceDir + '/' + n;
  return fs.stat(path).then(s => {
    const dateStr = n.replace(/.+(\d{4}-\d{2})\.pgn\.bz2/, '$1');
    const m = moment(dateStr);
    const shortName = n.replace(/.+(\d{4}-\d{2}.+)$/, '$1');
    const hasClock = m.unix() >= clockSince.unix();
    return {
      name: n,
      shortName: shortName,
      path: path,
      size: s.size,
      date: m,
      clock: hasClock,
      games: parseInt(gameCounts[n])
    };
  });
}

function getGameCounts() {
  return fs.readFile(sourceDir + '/counts.txt', { encoding: 'utf8' }).then(c => {
    var gameCounts = {};
    c.split('\n').map(l => l.trim()).forEach(line => {
      if (line !== '') gameCounts[line.split(' ')[0]] = line.split(' ')[1];
    });
    return gameCounts;
  });
}

function getFiles(gameCounts) {
  return fs.readdir(sourceDir).then(items => {
    return Promise.all(
      items.filter(n => n.includes('.pgn.bz2')).map(n => fileInfo(gameCounts, n))
    );
  }).then(items => items.sort((a, b) => b.date.unix() - a.date.unix()));
}

function renderTable(files) {
  return files.map(f => {
    return `<tr>
    <td>${f.date.format('MMMM YYYY')}</td>
    <td class="right">${prettyBytes(f.size)}</td>
    <td class="right">${numberFormat(f.games)}</td>
    <td class="center">${f.clock ? '✔' : ''}</td>
    <td><a href="${f.name}">${f.shortName}</a></td>
    </tr>`;
  }).join('\n');
}

function renderTotal(files) {
  return `<tr class="total">
  <td>Total: ${files.length} files</td>
  <td class="right">${prettyBytes(files.map(f => f.size).reduce((a, b) => a + b))}</td>
  <td class="right">${numberFormat(files.map(f => f.games).reduce((a, b) => a + b))}</td>
  <td></td>
  <td></td>
  </tr>`;
}

function renderList(files) {
  return files.map(f => {
    return `https://database.lichess.org/${f.name}`;
  }).join('\n');
}

Promise.all([
  getGameCounts().then(getFiles),
  fs.readFile(indexTpl, { encoding: 'utf8' }),
  fs.readFile(styleFile, { encoding: 'utf8' })
]).then(arr => {
  const rendered = arr[1]
    .replace(/<!-- files -->/, renderTable(arr[0]))
    .replace(/<!-- total -->/, renderTotal(arr[0]))
    .replace(/<!-- style -->/, arr[2]);
  return fs.writeFile(sourceDir + '/' + indexFile, rendered).then(_ => {
    return fs.writeFile(sourceDir + '/' + listFile, renderList(arr[0]));
  });
});
