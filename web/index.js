const fs = require('fs-extra');
const prettyBytes = require('pretty-bytes');
const moment = require('moment');

const sourceDir = process.argv[2];
const indexFile = 'index.html';
const indexTpl = indexFile + '.tpl';
const styleFile = 'style.css';

const clockSince = moment('2017-04');

const sizePerGameBeforeClock = 180;
const sizePerGameAfterClock = 260;

function numberFormat(n) {
  return new Intl.NumberFormat().format(n);
}

function fileInfo(n) {
  const path = sourceDir + '/' + n;
  return fs.stat(path).then(s => {
    const dateStr = n.replace(/.+(\d{4}-\d{2})\.pgn\.bz2/, '$1');
    const m = moment(dateStr);
    const shortName = n.replace(/.+(\d{4}-\d{2}.+)$/, '$1');
    const hasClock = m.unix() >= clockSince.unix();
    const sizePerGame = hasClock ? sizePerGameAfterClock : sizePerGameBeforeClock;
    return {
      name: n,
      shortName: shortName,
      path: path,
      size: s.size,
      date: m,
      clock: hasClock,
      games: Math.round(s.size / sizePerGame / 1000) * 1000
    };
  });
}

function getFiles() {
  return fs.readdir(sourceDir).then(items => {
    return Promise.all(
      items.filter(n => n.includes('.pgn.bz2')).map(fileInfo)
    );
  }).then(items => items.sort((a, b) => b.date.unix() - a.date.unix()));
}

function getIndex() {
  return fs.readFile(indexFile, { encoding: 'utf8' });
}

function renderTable(files) {
  return files.map(f => {
    return `<tr>
    <td>${f.date.format('MMMM YYYY')}</td>
    <td>${prettyBytes(f.size)}</td>
    <td>~${numberFormat(f.games)}</td>
    <td class="center">✔</td>
    <td class="center">${f.clock ? '✔' : ''}</td>
    <td><a href="${f.name}">${f.shortName}</a></td>
    </tr>`;
  }).join('\n');
}

function renderTotal(files) {
  return `<tr>
  <td>Total: ${files.length} files</td>
  <td>${prettyBytes(files.map(f => f.size).reduce((a, b) => a + b))}</td>
  <td>${numberFormat(files.map(f => f.games).reduce((a, b) => a + b))}</td>
  <td></td>
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
  getFiles(),
  fs.readFile(indexTpl, { encoding: 'utf8' }),
  fs.readFile(styleFile, { encoding: 'utf8' })
]).then(arr => {
  const rendered = arr[1]
    .replace(/<!-- files -->/, renderTable(arr[0]))
    .replace(/<!-- total -->/, renderTotal(arr[0]))
    .replace(/<!-- list -->/, renderList(arr[0]))
    .replace(/<!-- style -->/, arr[2]);
  fs.writeFile(sourceDir + '/' + indexFile, rendered);
});
