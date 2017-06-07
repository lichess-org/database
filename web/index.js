const fs = require('fs-extra');
const prettyBytes = require('pretty-bytes');
const moment = require('moment');

const sourceDir = process.argv[2];
const indexFile = 'index.html';
const indexTpl = indexFile + '.tpl';
const styleFile = 'style.css';

function fileInfo(n) {
  const path = sourceDir + '/' + n;
  return fs.stat(path).then(s => {
    const dateStr = n.replace(/.+(\d{4}-\d{2})\.pgn\.bz2/, '$1');
    const m = moment(dateStr);
    const shortName = n.replace(/.+(\d{4}-\d{2}.+)$/, '$1');
    return {
      name: n,
      shortName: shortName,
      path: path,
      size: prettyBytes(s.size),
      date: m
    };
  });
}

function getFiles() {
  return fs.readdir(sourceDir).then(items => {
    return Promise.all(
      items.filter(n => n.includes('.pgn.bz2')).map(fileInfo)
    );
  }).then(items => items.sort((a, b) => a.date < b.date));
}

function getIndex() {
  return fs.readFile(indexFile, { encoding: 'utf8' });
}

function renderTable(files) {
  return files.map(f => {
    return `<tr>
    <td>${f.date.format('MMMM YYYY')}</td>
    <td>${f.size}</td>
    <td><a href="${f.name}">${f.shortName}</a></td>
    </tr>`;
  }).join('\n');
}

Promise.all([
  getFiles(),
  fs.readFile(indexTpl, { encoding: 'utf8' }),
  fs.readFile(styleFile, { encoding: 'utf8' })
]).then(arr => {
  console.log(arr[0]);
  const rendered = arr[1]
    .replace(/<!-- files -->/, renderTable(arr[0]))
    .replace(/<!-- style -->/, arr[2]);
  fs.writeFile(sourceDir + '/' + indexFile, rendered);
});
