/**
 * 段落定位 — 仅 SearchNext（官方 executeMethod，无 callCommand 沙箱问题）
 * 通信：docEditor.serviceCommand('legalLocate', payload)
 *       → Gateway.on('internalcommand')
 */
(function (window) {
  console.info('[legal-locate-v3] plugin loaded', new Date().toISOString());
  var MSG_LOCATE = 'legal:locate';
  var MSG_RESULT = 'legal:locate-result';
  var MSG_READY = 'legal:locate-ready';

  function postResult(requestId, found, reason) {
    try {
      window.top.postMessage(
        {
          type: MSG_RESULT,
          found: !!found,
          requestId: requestId,
          reason: reason || (found ? 'ok' : 'not-found'),
        },
        '*',
      );
    } catch (e) {
      // ignore
    }
  }

  function signalReady() {
    try {
      window.top.postMessage({ type: MSG_READY }, '*');
    } catch (e) {
      // ignore
    }
  }

  function searchVariants(variants, index, requestId) {
    if (!variants || index >= variants.length) {
      postResult(requestId, false, 'search-miss');
      return;
    }
    var text = variants[index];
    if (!text) {
      searchVariants(variants, index + 1, requestId);
      return;
    }
    window.Asc.plugin.executeMethod(
      'SearchNext',
      [{ searchString: text, matchCase: false }, true],
      function (found) {
        if (found) {
          postResult(requestId, true, 'search');
        } else {
          searchVariants(variants, index + 1, requestId);
        }
      },
    );
  }

  function handleLocate(raw) {
    if (!raw) {
      return;
    }
    if (typeof raw === 'string') {
      try {
        raw = JSON.parse(raw);
      } catch (e) {
        return;
      }
    }
    if (raw.command === 'legalLocate' && raw.data) {
      raw = raw.data;
      if (typeof raw === 'string') {
        try {
          raw = JSON.parse(raw);
        } catch (e) {
          return;
        }
      }
    }
    if (raw.type !== MSG_LOCATE) {
      return;
    }
    var variants = raw.variants || [];
    if (variants.length === 0 && raw.bookmarkName) {
      variants = [String(raw.bookmarkName).replace(/^laby_p_/, '')];
    }
    searchVariants(variants, 0, raw.requestId);
  }

  function bindGateway() {
    try {
      var parent = window.parent;
      if (parent && parent.Common && parent.Common.Gateway) {
        parent.Common.Gateway.on('internalcommand', handleLocate);
        return true;
      }
    } catch (e) {
      // ignore
    }
    return false;
  }

  window.Asc.plugin.init = function () {
    if (!bindGateway()) {
      var n = 0;
      var timer = window.setInterval(function () {
        n += 1;
        if (bindGateway() || n >= 40) {
          window.clearInterval(timer);
        }
      }, 300);
    }
    signalReady();
  };

  window.Asc.plugin.onExternalPluginMessage = handleLocate;
  window.Asc.plugin.event_onDocumentReady = function () {
    bindGateway();
    signalReady();
  };
  window.Asc.plugin.button = function () {};
})(window);
