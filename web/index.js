const fs = require('fs-extra');
const prettyBytes = require('pretty-bytes');
const moment = require('moment');

const sourceDir = process.argv[2];
const indexFile = 'index.html';
const indexTpl = indexFile + '.tpl';
const tableTpl = 'table.html.tpl';
const styleFile = 'style.css';
const listFile = 'list.txt';

const clockSince = moment('2017-04');
const today = new Date();

function numberFormat(n) {
  return new Intl.NumberFormat().format(n);
}

function fileInfo(gameCounts, variant, n) {
  const path = sourceDir + '/' + variant + '/' + n;
  return fs.stat(path).then(s => {
    const dateStr = n.replace(/.+(\d{4}-\d{2})\.pgn\.zst/, '$1');
    const m = moment(dateStr);
    const hasClock = m.unix() >= clockSince.unix();
    return {
      name: n,
      path: path,
      size: s.size,
      date: m,
      clock: hasClock,
      games: parseInt(gameCounts[n]) || 0,
    };
  });
}

function getGameCounts(variant) {
  return fs.readFile(sourceDir + '/' + variant + '/counts.txt', { encoding: 'utf8' }).then(c => {
    var gameCounts = {};
    c.split('\n')
      .map(l => l.trim())
      .forEach(line => {
        if (line !== '') gameCounts[line.split(' ')[0]] = line.split(' ')[1];
      });
    return gameCounts;
  });
}

function getFiles(variant) {
  return function (gameCounts) {
    return fs
      .readdir(sourceDir + '/' + variant)
      .then(items => {
        return Promise.all(items.filter(n => n.endsWith('.pgn.zst')).map(n => fileInfo(gameCounts, variant, n)));
      })
      .then(items => items.sort((a, b) => b.date.unix() - a.date.unix()));
  };
}

function renderTable(files, variant) {
  return files
    .map(f => {
      return `<tr>
    <td>${f.date.format('YYYY - MMMM')}</td>
    <td class="right">${prettyBytes(f.size)}</td>
    <td class="right">${f.games ? numberFormat(f.games) : '?'}</td>
    <td><a href="${variant}/${f.name}">.pgn.zst</a> <span class="sep">/</span> <a href="${variant}/${
      f.name
    }.torrent">.torrent</a></td>
    </tr>`;
    })
    .join('\n');
}

function renderTotal(files) {
  return `<tr class="total">
  <td>Total: ${files.length} files</td>
  <td class="right">${prettyBytes(files.map(f => f.size).reduce((a, b) => a + b, 0))}</td>
  <td class="right">${numberFormat(files.map(f => f.games).reduce((a, b) => a + b, 0))}</td>
  <td></td>
  <td></td>
  </tr>`;
}

function renderList(files, variant) {
  return files
    .map(f => {
      return `https://database.lichess.org/${variant}/${f.name}`;
    })
    .join('\n');
}

function processVariantAndReturnTable(variant, template) {
  return getGameCounts(variant)
    .then(getFiles(variant))
    .then(files => {
      return fs.writeFile(sourceDir + '/' + variant + '/' + listFile, renderList(files, variant)).then(_ => {
        return template
          .replace(/<!-- nbGames -->/, numberFormat(files.map(f => f.games).reduce((a, b) => a + b, 0)))
          .replace(/<!-- files -->/, renderTable(files, variant))
          .replace(/<!-- total -->/, renderTotal(files))
          .replace(/<!-- variant -->/g, variant);
      });
    });
}

function replaceVariant(variant, tableTemplate) {
  return function (fullTemplate) {
    return processVariantAndReturnTable(variant, tableTemplate).then(tbl => {
      return fullTemplate.replace('<!-- table-' + variant + ' -->', tbl);
    });
  };
}

function replaceNbPuzzles(template) {
  return fs
    .readFile(sourceDir + '/puzzle-count.txt', { encoding: 'utf8' })
    .then(c => template.replace('<!-- nbPuzzles -->', numberFormat(c)));
}

function replaceNbEvals(template) {
  return fs
    .readFile(sourceDir + '/eval-count.txt', { encoding: 'utf8' })
    .then(c => template.replace('<!-- nbEvals -->', numberFormat(c)));
}

function replaceDateUpdated(template) {
  return template.replace('<!-- dateUpdated -->', today.toLocaleDateString('en-GB'));
}

process.on('unhandledRejection', r => console.log(r));

Promise.all([
  fs.readFile(indexTpl, { encoding: 'utf8' }),
  fs.readFile(tableTpl, { encoding: 'utf8' }),
  fs.readFile(styleFile, { encoding: 'utf8' }),
]).then(([index, table, style]) => {
  const rv = variant => replaceVariant(variant, table);
  return rv('standard')(index)
    .then(rv('antichess'))
    .then(rv('atomic'))
    .then(rv('chess960'))
    .then(rv('crazyhouse'))
    .then(rv('horde'))
    .then(rv('kingOfTheHill'))
    .then(rv('racingKings'))
    .then(rv('threeCheck'))
    .then(replaceNbPuzzles)
    .then(replaceNbEvals)
    .then(replaceDateUpdated)
    .then(rendered => {
      return fs.writeFile(sourceDir + '/' + indexFile, rendered.replace(/<!-- style -->/, style));
    });
});
