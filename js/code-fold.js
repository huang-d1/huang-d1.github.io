/* global CONFIG */

(function() {
  'use strict';

  function getOptions() {
    var options = CONFIG.code_fold || {};
    return {
      enable: options.enable !== false,
      defaultOpen: options.default_open === true,
      title: options.title || '展开代码'
    };
  }

  function getLanguage(block) {
    var language = block.querySelector('[data-language]');
    if (language) {
      return language.getAttribute('data-language');
    }
    var className = block.className || '';
    var match = className.match(/(?:^|\s)(?:lang(?:uage)?-|highlight\s+)([\w+-]+)/i);
    return match ? match[1] : '';
  }

  function foldCodeBlocks() {
    var options = getOptions();
    if (!options.enable || !document.querySelectorAll) {
      return;
    }

    var blocks = document.querySelectorAll('.markdown-body figure.highlight, .markdown-body pre');
    Array.prototype.forEach.call(blocks, function(block) {
      if ((block.tagName === 'PRE' && block.closest('figure.highlight'))
        || block.closest('details, .fold')
        || (block.parentNode && block.parentNode.classList.contains('code-fold'))) {
        return;
      }

      var details = document.createElement('details');
      details.className = 'code-fold';
      details.open = options.defaultOpen;

      var summary = document.createElement('summary');
      var language = getLanguage(block);
      summary.textContent = language ? options.title + '（' + language.toUpperCase() + '）' : options.title;
      summary.setAttribute('aria-label', summary.textContent);
      details.appendChild(summary);

      block.parentNode.insertBefore(details, block);
      details.appendChild(block);
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', foldCodeBlocks);
  } else {
    foldCodeBlocks();
  }

  window.FluidCodeFold = foldCodeBlocks;
})();
