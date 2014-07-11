/// <reference path="jquery.d.ts" />
if (typeof Array.prototype.indexOf !== "function") {
    Array.prototype.indexOf = function (searchElement, fromIndex) {
        var from = (typeof fromIndex === "number" ? fromIndex : 0);
        for (var i = from; i < this.length; i++) {
            if (this[i] === searchElement)
                return i;
        }
        return -1;
    };
}

var tui;
(function (tui) {
    tui.undef = (function (undefined) {
        return typeof undefined;
    })();

    tui.undefVal = (function (undefined) {
        return undefined;
    })();

    tui.lang = (function () {
        return (navigator.language || navigator.browserLanguage || navigator.userLanguage).toLowerCase();
    })();

    var _translate = {};

    /**
    * Register a translation engine.
    */
    function registerTranslator(lang, func) {
        _translate[lang] = func;
    }
    tui.registerTranslator = registerTranslator;

    /**
    * Multi-language support, translate source text to specified language(default use tui.lang setting)
    * @param str {string} source text
    * @param lang {string} if specified then use this parameter as objective language otherwise use tui.lang as objective language
    */
    function str(str, lang) {
        if (!lang) {
            if (!tui.lang)
                return str;
            else
                lang = tui.lang;
        }
        var func = _translate[lang];
        if (typeof func === "function") {
            return func(str);
        } else
            return str;
    }
    tui.str = str;

    tui.uuid = (function () {
        var id = 0;
        return function () {
            var uid = 'tuid' + id++;
            return uid;
        };
    })();

    /**
    * Base object, all other control extended from this base class.
    */
    var EventObject = (function () {
        function EventObject() {
            this._events = {};
        }
        EventObject.prototype.bind = function (eventName, handler, priority) {
            if (!eventName)
                return;
            if (!this._events[eventName]) {
                this._events[eventName] = [];
            }
            var handlers = this._events[eventName];
            for (var i = 0; i < handlers.length; i++) {
                if (handlers[i] === handler)
                    return;
            }
            if (priority)
                handlers.push(handler);
            else
                handlers.splice(0, 0, handler);
        };

        EventObject.prototype.unbind = function (eventName, handler) {
            if (!eventName)
                return;
            var handlers = this._events[eventName];
            if (handler) {
                for (var i = 0; i < handlers.length; i++) {
                    if (handler === handlers[i]) {
                        handlers.splice(i, 1);
                        return;
                    }
                }
            } else {
                handlers.length = 0;
            }
        };

        /**
        * Register event handler.
        * @param {string} eventName
        * @param {callback} callback Which handler to be registered
        * @param {boolean} priority If true then handler will be triggered firstly
        */
        EventObject.prototype.on = function (eventName, callback, priority) {
            if (typeof priority === "undefined") { priority = false; }
            var envs = eventName.split(/\s+/);
            for (var i = 0; i < envs.length; i++) {
                var v = envs[i];
                this.bind(v, callback, priority);
            }
        };

        /**
        * Register event handler.
        * @param eventName
        * @param callback Which handler to be registered but event only can be trigered once
        * @param priority If true then handler will be triggered firstly
        */
        EventObject.prototype.once = function (eventName, callback, priority) {
            if (typeof priority === "undefined") { priority = false; }
            callback.isOnce = true;
            this.on(eventName, callback, priority);
        };

        /**
        * Unregister event handler.
        * @param eventName
        * @param callback Which handler to be unregistered if don't specified then unregister all handler
        */
        EventObject.prototype.off = function (eventName, callback) {
            var envs = eventName.split(/\s+/);
            for (var i = 0; i < envs.length; i++) {
                var v = envs[i];
                this.unbind(v, callback);
            }
        };

        /**
        * Fire event. If some handler process return false then cancel the event channe and return false either
        * @param {string} eventName
        * @param {any[]} param
        */
        EventObject.prototype.fire = function (eventName, data) {
            // srcElement: HTMLElement, e?: JQueryEventObject, ...param: any[]
            var array = this._events[eventName];
            if (!array) {
                return;
            }
            var _data = null;
            if (data) {
                _data = data;
                _data["name"] = eventName;
            } else
                _data = { "name": eventName };
            var removeArray = [];
            for (var i = 0; i < array.length; i++) {
                var handler = array[i];
                if (handler.isOnce)
                    removeArray.push(handler);
                var val = handler.call(this, _data);
                if (typeof val === "boolean" && !val)
                    return false;
            }
            for (var i = 0; i < removeArray.length; i++) {
                this.off(eventName, removeArray[i]);
            }
        };
        return EventObject;
    })();
    tui.EventObject = EventObject;

    var _eventObject = new EventObject();
    function on(eventName, callback, priority) {
        if (typeof priority === "undefined") { priority = false; }
        _eventObject.on(eventName, callback, priority);
    }
    tui.on = on;
    function once(eventName, callback, priority) {
        if (typeof priority === "undefined") { priority = false; }
        _eventObject.once(eventName, callback, priority);
    }
    tui.once = once;
    function off(eventName, callback) {
        _eventObject.off(eventName, callback);
    }
    tui.off = off;
    function fire(eventName, data) {
        return EventObject.prototype.fire.call(_eventObject, eventName, data);
    }
    tui.fire = fire;

    function parseBoolean(string) {
        if (typeof string === tui.undef)
            return false;
        switch (String(string).toLowerCase()) {
            case "true":
            case "1":
            case "yes":
            case "y":
                return true;
            default:
                return false;
        }
    }
    tui.parseBoolean = parseBoolean;

    function toElement(html) {
        var div = document.createElement('div');
        div.innerHTML = html;
        var el = div.firstChild;
        return div.removeChild(el);
    }
    tui.toElement = toElement;

    function removeNode(node) {
        node.parentNode && node.parentNode.removeChild(node);
    }
    tui.removeNode = removeNode;

    /**
    * Get or set a HTMLElement's text content, return Element's text content.
    * @param elem {HTMLElement or ID of the element} Objective element
    * @param text {string or other object that can be translated to string}
    */
    function elementText(elem, text) {
        if (typeof elem === "string")
            elem = document.getElementById(elem);
        if (elem) {
            if (typeof text !== "undefined") {
                elem.innerHTML = "";
                elem.appendChild(document.createTextNode(text));
                return text;
            }
            if (typeof elem.textContent !== "undefined")
                return elem.textContent;
            var buf = "";
            for (var i = 0; i < elem.childNodes.length; i++) {
                var c = elem.childNodes[i];
                if (c.nodeName.toLowerCase() === "#text") {
                    buf += c.nodeValue;
                } else
                    buf += elementText(c);
            }
            return buf;
        } else
            return null;
    }
    tui.elementText = elementText;

    function fixedPosition(target) {
        var $target = $(target);
        var offset = $target.offset();
        var $doc = $(document);
        return {
            x: offset.left - $doc.scrollLeft(),
            y: offset.top - $doc.scrollTop()
        };
    }
    tui.fixedPosition = fixedPosition;

    function debugElementPosition(target) {
        $(target).mousedown(function (e) {
            var pos = tui.fixedPosition(this);
            var anchor = document.createElement("span");
            anchor.style.backgroundColor = "#ccc";
            anchor.style.opacity = "0.5";
            anchor.style.display = "inline-block";
            anchor.style.position = "fixed";
            anchor.style.left = pos.x + "px";
            anchor.style.top = pos.y + "px";
            anchor.style.width = this.offsetWidth + "px";
            anchor.style.height = this.offsetHeight + "px";
            document.body.appendChild(anchor);
            $(anchor).mouseup(function (e) {
                document.body.removeChild(anchor);
            });
            // console.log(tui.format("x: {0}, y: {1}", pos.x, pos.y));
        });
    }
    tui.debugElementPosition = debugElementPosition;

    /**
    * Obtain hosted document's window size
    */
    function windowSize() {
        var w = 630, h = 460;
        if (document.body && document.body.offsetWidth) {
            w = document.body.offsetWidth;
            h = document.body.offsetHeight;
        }
        if (document.compatMode === 'CSS1Compat' && document.documentElement && document.documentElement.offsetWidth) {
            w = document.documentElement.offsetWidth;
            h = document.documentElement.offsetHeight;
        }
        if (window.innerWidth && window.innerHeight) {
            w = window.innerWidth;
            h = window.innerHeight;
        }
        return { width: w, height: h };
    }
    tui.windowSize = windowSize;
    ;

    /**
    * Get top window's body element
    */
    function getTopBody() {
        return top.document.body || top.document.getElementsByTagName("BODY")[0];
    }
    tui.getTopBody = getTopBody;

    /**
    * Get element's owner window
    */
    function getWindow(elem) {
        return elem.ownerDocument.defaultView || elem.ownerDocument.parentWindow || elem.ownerDocument.Script;
    }
    tui.getWindow = getWindow;

    /**
    * Deeply copy an object to an other object, but only contain properties without methods
    */
    function clone(obj) {
        return JSON.parse(JSON.stringify(obj));
    }
    tui.clone = clone;

    /**
    * Test whether the button code is indecated that the event is triggered by a left mouse button.
    */
    function isLButton(buttonCode) {
        if (tui.ieVer !== -1 && tui.ieVer < 9) {
            return (buttonCode === 1);
        } else {
            return buttonCode === 0;
        }
    }
    tui.isLButton = isLButton;

    /**
    * Prevent user press backspace key to go back to previous page
    */
    function banBackspace() {
        function ban(e) {
            var ev = e || window.event;
            var obj = ev.target || ev.srcElement;
            var t = obj.type || obj.getAttribute('type');
            var vReadOnly = obj.readOnly;
            var vDisabled = obj.disabled;
            vReadOnly = (typeof vReadOnly === tui.undef) ? false : vReadOnly;
            vDisabled = (typeof vDisabled === tui.undef) ? true : vDisabled;
            var flag1 = ev.keyCode === 8 && (t === "password" || t === "text" || t === "textarea") && (vReadOnly || vDisabled);
            var flag2 = ev.keyCode === 8 && t !== "password" && t !== "text" && t !== "textarea";
            if (flag2 || flag1)
                return false;
        }
        $(document).bind("keypress", ban);
        $(document).bind("keydown", ban);
    }
    tui.banBackspace = banBackspace;

    /**
    * Detect whether the given parent element is the real ancestry element
    * @param elem
    * @param parent
    */
    function isAncestry(elem, parent) {
        while (elem) {
            if (elem === parent)
                return true;
            else
                elem = elem.parentNode;
        }
        return false;
    }
    tui.isAncestry = isAncestry;

    /**
    * Detect whether the given child element is the real posterity element
    * @param elem
    * @param child
    */
    function isPosterity(elem, child) {
        return isAncestry(child, elem);
    }
    tui.isPosterity = isPosterity;

    /**
    * Detect whether the element is inside the document
    * @param {type} elem
    */
    function isInDoc(elem) {
        var obj = elem;
        while (obj) {
            if (obj.nodeName.toUpperCase() === "HTML")
                return true;
            obj = obj.parentElement;
        }
        return false;
    }
    tui.isInDoc = isInDoc;

    /**
    * Format a string use a set of parameters
    */
    function format(token) {
        var params = [];
        for (var _i = 0; _i < (arguments.length - 1); _i++) {
            params[_i] = arguments[_i + 1];
        }
        var formatrg = /\{(\d+)\}/g;
        token && (typeof token === "string") && params.length && (token = token.replace(formatrg, function (str, i) {
            return params[i] === null ? "" : params[i];
        }));
        return token ? token : "";
    }
    tui.format = format;

    /**
    * Format a number that padding it with '0'
    */
    function paddingNumber(v, min, max, alignLeft) {
        if (typeof alignLeft === "undefined") { alignLeft = false; }
        var result = Math.abs(v) + "";
        while (result.length < min) {
            result = "0" + result;
        }
        if (typeof max === "number" && result.length > max) {
            if (alignLeft)
                result = result.substr(0, max);
            else
                result = result.substr(result.length - max, max);
        }
        if (v < 0)
            result = "-" + result;
        return result;
    }
    tui.paddingNumber = paddingNumber;

    /**
    * Get the parameter of the URL query string.
    * @param {String} url
    * @param {String} key Parameter name
    */
    function getParam(url, key) {
        key = key.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
        var regex = new RegExp("[\\?&]" + key + "=([^&#]*)"), results = regex.exec(url);
        return results === null ? null : decodeURIComponent(results[1].replace(/\+/g, " "));
    }
    tui.getParam = getParam;

    var BackupedScrollPosition = (function () {
        function BackupedScrollPosition(target) {
            this.backupInfo = [];
            var obj = target;
            while (obj && obj !== document.body) {
                obj = obj.parentElement;
                if (obj)
                    this.backupInfo.push({ obj: obj, left: obj.scrollLeft, top: obj.scrollTop });
            }
        }
        BackupedScrollPosition.prototype.restore = function () {
            for (var i = 0; i < this.backupInfo.length; i++) {
                var item = this.backupInfo[i];
                item.obj.scrollLeft = item.left;
                item.obj.scrollTop = item.top;
            }
        };
        return BackupedScrollPosition;
    })();
    tui.BackupedScrollPosition = BackupedScrollPosition;

    function backupScrollPosition(target) {
        return new BackupedScrollPosition(target);
    }
    tui.backupScrollPosition = backupScrollPosition;

    function focusWithoutScroll(target) {
        setTimeout(function () {
            if (tui.ieVer > 0) {
                //if (tui.ieVer > 8)
                //	target.setActive();
                //else {
                //	if (target !== document.activeElement)
                target.setActive();
                //}
            } else if (tui.ffVer > 0)
                target.focus();
            else {
                var backup = tui.backupScrollPosition(target);
                target.focus();
                backup.restore();
            }
        }, 0);
    }
    tui.focusWithoutScroll = focusWithoutScroll;

    function scrollToElement(elem) {
        var obj = elem;
        while (obj) {
            var parent = obj.offsetParent;
            $(parent).animate({ scrollTop: $(obj).offset().top }, 200);
            obj = parent;
        }
    }
    tui.scrollToElement = scrollToElement;

    /**
    * Get IE version
    * @return {Number}
    */
    tui.ieVer = (function () {
        var rv = -1;
        if (navigator.appName === "Microsoft Internet Explorer" || navigator.appName === "Netscape") {
            var ua = navigator.userAgent;
            var re = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
            if (re.exec(ua) !== null)
                rv = parseFloat(RegExp.$1);
        }
        if (rv === -1 && navigator.appName === "Netscape") {
            var ua = navigator.userAgent;
            var re = new RegExp("Trident/([0-9]{1,}[\.0-9]{0,})");
            if (re.exec(ua) !== null)
                rv = parseFloat(RegExp.$1);
            if (rv >= 7.0)
                rv = 11.0;
        }
        return rv;
    })();

    /**
    * Get Firefox version
    * @return {Number}
    */
    tui.ffVer = (function () {
        var rv = -1;
        if (navigator.appName === "Netscape") {
            var ua = navigator.userAgent;
            var re = new RegExp("Firefox/([0-9]{1,}[\.0-9]{0,})");
            if (re.exec(ua) !== null)
                rv = parseFloat(RegExp.$1);
        }
        return rv;
    })();

    /**
    * Set cookie value
    * @param name
    * @param value
    * @param days valid days
    */
    function saveCookie(name, value, expires, path, domain, secure) {
        if (typeof secure === "undefined") { secure = false; }
        // set time, it's in milliseconds
        var today = new Date();
        today.setTime(today.getTime());

        /*
        if the expires variable is set, make the correct
        expires time, the current script below will set
        it for x number of days, to make it for hours,
        delete * 24, for minutes, delete * 60 * 24
        */
        if (expires) {
            expires = expires * 1000 * 60 * 60 * 24;
        }
        var expires_date = new Date(today.getTime() + (expires));
        document.cookie = name + "=" + encodeURIComponent(JSON.stringify(value)) + ((expires) ? ";expires=" + expires_date.toUTCString() : "") + ((path) ? ";path=" + path : "") + ((domain) ? ";domain=" + domain : "") + ((secure) ? ";secure" : "");
    }
    tui.saveCookie = saveCookie;

    /**
    * Get cookie value
    * @param name
    */
    function loadCookie(name) {
        var arr = document.cookie.match(new RegExp("(^| )" + name + "=([^;]*)(;|$)"));
        if (arr !== null)
            return JSON.parse(decodeURIComponent(arr[2]));
        else
            return null;
    }
    tui.loadCookie = loadCookie;

    /**
    * Delete cookie
    * @param name
    */
    function deleteCookie(name, path, domain) {
        if (loadCookie(name))
            document.cookie = name + "=" + ((path) ? ";path=" + path : "") + ((domain) ? ";domain=" + domain : "") + ";expires=Thu, 01-Jan-1970 00:00:01 GMT";
    }
    tui.deleteCookie = deleteCookie;

    /**
    * Save key value into local storage, if local storage doesn't usable then use local cookie instead.
    * @param {String} key
    * @param {String} value
    * @param {Boolean} sessionOnly If true data only be keeped in this session
    */
    function saveData(key, value, sessionOnly) {
        if (typeof sessionOnly === "undefined") { sessionOnly = false; }
        try  {
            var storage = (sessionOnly === true ? window.sessionStorage : window.localStorage);
            if (storage) {
                storage.setItem(key, JSON.stringify(value));
            } else
                saveCookie(key, value, 365);
        } catch (e) {
        }
    }
    tui.saveData = saveData;

    /**
    * Load value from local storage, if local storage doesn't usable then use local cookie instead.
    * @param {String} key
    * @param {Boolean} sessionOnly If true data only be keeped in this session
    */
    function loadData(key, sessionOnly) {
        if (typeof sessionOnly === "undefined") { sessionOnly = false; }
        try  {
            var storage = (sessionOnly === true ? window.sessionStorage : window.localStorage);
            if (storage)
                return JSON.parse(storage.getItem(key));
            else
                return loadCookie(key);
        } catch (e) {
            return null;
        }
    }
    tui.loadData = loadData;

    /**
    * Remove value from local storage, if local storage doesn't usable then use local cookie instead.
    * @param key
    * @param {Boolean} sessionOnly If true data only be keeped in this session
    */
    function deleteData(key, sessionOnly) {
        if (typeof sessionOnly === "undefined") { sessionOnly = false; }
        try  {
            var storage = (sessionOnly === true ? window.sessionStorage : window.localStorage);
            if (storage)
                storage.removeItem(key);
            else
                deleteCookie(key);
        } catch (e) {
        }
    }
    tui.deleteData = deleteData;
})(tui || (tui = {}));
/// <reference path="tui.core.ts" />
var tui;
(function (tui) {
    var shortWeeks = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
    var weeks = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
    var shortMonths = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
    var months = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];

    /**
    * Get today
    */
    function today() {
        return new Date();
    }
    tui.today = today;

    /**
    * Input seconds and get a time description
    * @param seconds Tims distance of seconds
    * @param lang Display language
    */
    function timespan(seconds, lang) {
        var desc = ["day", "hour", "minute", "second"];
        var val = [];
        var beg = "", end = "";
        var d = Math.floor(seconds / 86400);
        val.push(d);
        seconds = seconds % 86400;
        var h = Math.floor(seconds / 3600);
        val.push(h);
        seconds = seconds % 3600;
        var m = Math.floor(seconds / 60);
        val.push(m);
        val.push(seconds % 60);
        var i = 0, j = 3;
        while (i < 4) {
            if (val[i] > 0) {
                beg.length && (beg += " ");
                beg += val[i] + " " + tui.str(val[i] > 1 ? desc[i] + "s" : desc[i], lang);
                break;
            }
            i++;
        }
        while (i < j) {
            if (val[j] > 0) {
                end.length && (end += " ");
                end += val[j] + " " + tui.str(val[j] > 1 ? desc[j] + "s" : desc[j], lang);
                break;
            }
            j--;
        }
        i++;
        while (i < j) {
            beg.length && (beg += " ");
            beg += val[i] + " " + tui.str(val[i] > 1 ? desc[i] + "s" : desc[i], lang);
            i++;
        }
        return beg + (beg.length ? " " : "") + end;
    }
    tui.timespan = timespan;

    /**
    * Get the distance of dt2 compare to dt1 (dt2 - dt1) return in specified unit (d: day, h: hours, m: minutes, s: seconds, ms: milliseconds)
    * @param dt1
    * @param dt2
    * @param unit "d", "h", "m", "s" or "ms"
    */
    function dateDiff(dt1, dt2, unit) {
        if (typeof unit === "undefined") { unit = "d"; }
        var d1 = dt1.getTime();
        var d2 = dt2.getTime();
        var diff = d2 - d1;
        var symbol = diff < 0 ? -1 : 1;
        diff = Math.abs(diff);
        unit = unit.toLocaleLowerCase();
        if (unit === "d") {
            return Math.floor(diff / 86400000) * symbol;
        } else if (unit === "h") {
            return Math.floor(diff / 3600000) * symbol;
        } else if (unit === "m") {
            return Math.floor(diff / 60000) * symbol;
        } else if (unit === "s") {
            return Math.floor(diff / 1000) * symbol;
        } else if (unit === "ms") {
            return diff * symbol;
        } else
            return NaN;
    }
    tui.dateDiff = dateDiff;

    /**
    * Get new date of dt add specified unit of values.
    * @param dt The day of the target
    * @param val Increased value
    * @param unit "d", "h", "m", "s" or "ms"
    */
    function dateAdd(dt, val, unit) {
        if (typeof unit === "undefined") { unit = "d"; }
        var d = dt.getTime();
        if (unit === "d") {
            return new Date(d + val * 86400000);
        } else if (unit === "h") {
            return new Date(d + val * 3600000);
        } else if (unit === "m") {
            return new Date(d + val * 60000);
        } else if (unit === "s") {
            return new Date(d + val * 1000);
        } else if (unit === "ms") {
            return new Date(d + val);
        } else
            return null;
    }
    tui.dateAdd = dateAdd;

    /**
    * Get day in year
    * @param dt The day of the target
    */
    function dayOfYear(dt) {
        var y = dt.getFullYear();
        var d1 = new Date(y, 0, 1);
        return dateDiff(d1, dt, "d");
    }
    tui.dayOfYear = dayOfYear;

    /**
    * Get total days of month
    * @param dt The day of the target
    */
    function totalDaysOfMonth(dt) {
        var y = dt.getFullYear();
        var m = dt.getMonth();
        var d1 = new Date(y, m, 1);
        if (m === 11) {
            y++;
            m = 0;
        } else {
            m++;
        }
        var d2 = new Date(y, m, 1);
        return dateDiff(d1, d2, "d");
    }
    tui.totalDaysOfMonth = totalDaysOfMonth;

    /**
    * Parse string get date instance (format: yyyy-MM-dd hh:mm:ss or ISO8601 format)
    * @param {String} dtStr Data string
    */
    function parseDate(dtStr) {
        var now = new Date();
        var year = now.getFullYear();
        var month = now.getMonth();
        var day = now.getDate();
        var hour = 0;
        var minute = 0;
        var second = 0;
        var millisecond = 0;
        var tz = new Date().getTimezoneOffset();
        var pyear = "(\\d{4})";
        var pmonth = "(1[0-2]|0?[1-9])";
        var pday = "(0?[1-9]|[12][0-9]|3[01])";
        var phour = "(0?[0-9]|1[0-9]|2[0-3])";
        var pminute = "([0-5]?[0-9])";
        var psecond = "([0-5]?[0-9])";
        var pmillisecond = "([0-9]+)";
        var ptz = "((?:\\+|-)(?:1[0-2]|0[0-9])(?:[0-5][0-9])?)";
        var isUTC = false;

        var regex = "^" + pyear + "-" + pmonth + "-" + pday + "(?:\\s+" + phour + "(?::" + pminute + "(?::" + psecond + ")?)?)?$";
        var matches = new RegExp(regex, "g").exec(dtStr);
        if (matches === null) {
            regex = "^" + pyear + "-" + pmonth + "-" + pday + "(?:T" + phour + ":" + pminute + ":" + psecond + "(?:\\." + pmillisecond + ")?Z)?$";
            matches = new RegExp(regex, "g").exec(dtStr);
            if (matches)
                isUTC = true;
        }
        if (matches === null) {
            regex = "^" + pyear + "-" + pmonth + "-" + pday + "(?:T" + phour + ":" + pminute + ":" + psecond + "(?:\\." + pmillisecond + ")?(?:" + ptz + ")?)?$";
            matches = new RegExp(regex, "g").exec(dtStr);
        }
        if (matches instanceof Array) {
            if (typeof matches[1] === "string" && matches[1].length > 0)
                year = parseInt(matches[1], 10);
            if (typeof matches[2] === "string" && matches[2].length > 0)
                month = parseInt(matches[2], 10) - 1;
            if (typeof matches[3] === "string" && matches[3].length > 0)
                day = parseInt(matches[3], 10);
            if (typeof matches[4] === "string" && matches[4].length > 0)
                hour = parseInt(matches[4], 10);
            if (typeof matches[5] === "string" && matches[5].length > 0)
                minute = parseInt(matches[5], 10);
            if (typeof matches[6] === "string" && matches[6].length > 0)
                second = parseInt(matches[6], 10);
            if (typeof matches[7] === "string" && matches[7].length > 0)
                millisecond = parseInt(matches[7], 10);
            if (typeof matches[8] === "string" && matches[8].length > 0) {
                tz = parseInt(matches[8].substr(1, 2), 10) * 60;
                if (matches[8].length >= 5)
                    tz += parseInt(matches[8].substr(3, 2), 10);
                if (matches[8].substr(0, 1) === "+")
                    tz = -tz;
            }
            if (isUTC)
                return new Date(Date.UTC(year, month, day, hour, minute, second, millisecond));
            else {
                return new Date(Date.UTC(year, month, day, hour, minute, second, millisecond) + tz * 60 * 1000);
            }
        } else
            return null;
    }
    tui.parseDate = parseDate;

    /**
    * Convert date to string and output can be formated to ISO8601, RFC2822, RFC3339 or other customized format
    * @param dt {Date} Date object to be convert
    * @param dateFmt {String} which format should be apply, default use ISO8601 standard format
    */
    function formatDate(dt, dateFmt) {
        if (typeof dateFmt === "undefined") { dateFmt = "yyyy-MM-ddTHH:mm:sszzz"; }
        var rule = {
            "y+": dt.getFullYear(),
            "M+": dt.getMonth() + 1,
            "d+": dt.getDate(),
            "D+": dayOfYear(dt),
            "h+": (function (h) {
                if (h === 0)
                    return h + 12;
                else if (h >= 1 && h <= 12)
                    return h;
                else if (h >= 13 && h <= 23)
                    return h - 12;
            })(dt.getHours()),
            "H+": dt.getHours(),
            "m+": dt.getMinutes(),
            "s+": dt.getSeconds(),
            "q+": Math.floor((dt.getMonth() + 3) / 3),
            "S+": dt.getMilliseconds(),
            "E+": dt.getDay(),
            "a": (function (h) {
                if (h >= 0 && h <= 11)
                    return "am";
                else
                    return "pm";
            })(dt.getHours()),
            "A": (function (h) {
                if (h >= 0 && h <= 11)
                    return "AM";
                else
                    return "PM";
            })(dt.getHours()),
            "z+": dt.getTimezoneOffset()
        };
        var regex = "";
        for (var k in rule) {
            if (regex.length > 0)
                regex += "|";
            regex += k;
        }
        var regexp = new RegExp(regex, "g");
        return dateFmt.replace(regexp, function (str, pos, source) {
            for (var k in rule) {
                if (str.match(k) !== null) {
                    if (k === "y+") {
                        return tui.paddingNumber(rule[k], str.length, str.length);
                    } else if (k === "a" || k === "A") {
                        return rule[k];
                    } else if (k === "z+") {
                        var z = "";
                        if (rule[k] >= 0) {
                            z += "-";
                        } else {
                            z += "+";
                        }
                        if (str.length < 2)
                            z += Math.abs(Math.floor(rule[k] / 60));
                        else
                            z += tui.paddingNumber(Math.abs(Math.floor(rule[k] / 60)), 2);
                        if (str.length === 3)
                            z += tui.paddingNumber(Math.abs(Math.floor(rule[k] % 60)), 2);
                        else if (str.length > 3)
                            z += (":" + tui.paddingNumber(Math.abs(Math.floor(rule[k] % 60)), 2));
                        return z;
                    } else if (k === "E+") {
                        if (str.length < 3)
                            return tui.paddingNumber(rule[k], str.length);
                        else if (str.length === 3)
                            return shortWeeks[rule[k]];
                        else
                            return weeks[rule[k]];
                    } else if (k === "M+") {
                        if (str.length < 3)
                            return tui.paddingNumber(rule[k], str.length);
                        else if (str.length === 3)
                            return shortMonths[rule[k] - 1];
                        else
                            return months[rule[k] - 1];
                    } else if (k === "S+") {
                        return tui.paddingNumber(rule[k], str.length, str.length, true);
                    } else {
                        return tui.paddingNumber(rule[k], str.length);
                    }
                }
            }
            return str;
        });
    }
    tui.formatDate = formatDate;
})(tui || (tui = {}));
var __extends = this.__extends || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
};
/// <reference path="tui.core.ts" />
var tui;
(function (tui) {
    var mimeTypeMap = {
        "application/java-archive": ["jar"],
        "application/msword": ["doc"],
        "application/pdf": ["pdf"],
        "application/pkcs10": ["p10"],
        "application/pkcs7-mime": ["p7m"],
        "application/pkcs7-signature": ["p7s"],
        "application/postscript": ["ai"],
        "application/vnd.ms-excel": ["xls"],
        "application/vnd.ms-powerpoint": ["ppt"],
        "application/vnd.ms-project": ["mpp"],
        "application/vnd.ms-visio.viewer": ["vsd"],
        "application/vnd.ms-xpsdocument": ["xps"],
        "application/vnd.oasis.opendocument.presentation": ["odp"],
        "application/vnd.oasis.opendocument.spreadsheet": ["ods"],
        "application/vnd.oasis.opendocument.text": ["odt"],
        "application/vnd.openxmlformats-officedocument.presentationml.presentation": ["pptx"],
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": ["xlsx"],
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document": ["docx"],
        "application/x-7z-compressed": ["7z"],
        "application/x-bzip2": ["bz2"],
        "application/x-gzip": ["gz"],
        "application/x-javascript": ["js"],
        "application/x-msdownload": ["exe"],
        "application/x-msmetafile": ["wmf"],
        "application/x-pkcs12": ["p12", "pfx"],
        "application/x-pkcs7-certificates": ["p7b"],
        "application/x-pkcs7-certreqresp": ["p7r"],
        "application/x-rar-compressed": ["rar"],
        "application/x-shockwave-flash": ["swf"],
        "application/x-tar": ["tar"],
        "application/x-x509-ca-cert": ["cer"],
        "application/xhtml+xml": ["xhtml"],
        "application/xml": ["xml"],
        "application/zip": ["zip"],
        "audio/mp4": ["m4a"],
        "audio/mpeg": ["mp3"],
        "audio/x-ms-wma": ["wma"],
        "audio/x-pn-realaudio": ["rm"],
        "image/bmp": ["bmp"],
        "image/gif": ["gif"],
        "image/jpeg": ["jpeg"],
        "image/nbmp": ["nbmp"],
        "image/png": ["png"],
        "image/svg-xml": ["svg"],
        "image/tiff": ["tiff"],
        "image/x-icon": ["ico"],
        "message/rfc822": ["eml"],
        "text/css": ["css"],
        "text/html": ["html"],
        "text/plain": ["txt"],
        "text/xml": ["xml"],
        "video/3gpp": ["3gp"],
        "video/3gpp2": ["3gp2"],
        "video/avi": ["avi"],
        "video/mp4": ["mp4"],
        "video/mpeg": ["mpeg"],
        "video/quicktime": ["mov"]
    };
    function checkExtWithMimeType(ext, mimeType) {
        var exts = mimeTypeMap[mimeType];
        if (typeof exts === tui.undef) {
            return true;
        }
        if (exts.indexOf(ext.toLowerCase()) >= 0)
            return true;
        else
            return false;
    }
    tui.checkExtWithMimeType = checkExtWithMimeType;

    function getBox(el) {
        var left, right, top, bottom;
        var offset = $(el).position();
        left = offset.left;
        top = offset.top;
        right = left + el.offsetWidth;
        bottom = top + el.offsetHeight;
        return {
            left: left,
            right: right,
            top: top,
            bottom: bottom
        };
    }

    function copyLayout(from, to) {
        var box = getBox(from);
        $(to).css({
            position: 'absolute',
            left: box.left + 'px',
            top: box.top + 'px',
            width: from.offsetWidth + 'px',
            height: from.offsetHeight + 'px'
        });
    }

    function fileFromPath(file) {
        return file.replace(/.*(\/|\\)/, "");
    }

    function getExt(file) {
        return (-1 !== file.indexOf('.')) ? file.replace(/.*[.]/, '') : '';
    }

    function preventDefault(e) {
        return e.preventDefault();
    }

    var UploadBinding = (function (_super) {
        __extends(UploadBinding, _super);
        function UploadBinding(button, options) {
            _super.call(this);
            this._settings = {
                action: "upload",
                name: "userfile",
                multiple: false,
                autoSubmit: true,
                responseType: "auto",
                hoverClass: "tui-input-btn-hover",
                focusClass: "tui-input-btn-active",
                disabledClass: "tui-input-btn-disabled"
            };
            this._button = null;
            this._input = null;
            this._disabled = false;
            if (options) {
                for (var i in options) {
                    if (options.hasOwnProperty(i)) {
                        this._settings[i] = options[i];
                    }
                }
            }
            if (typeof button === "string") {
                if (/^#.*/.test(button)) {
                    // If jQuery user passes #elementId don't break it
                    button = button.slice(1);
                }
                button = document.getElementById(button);
            }
            if (!button || button.nodeType !== 1) {
                throw new Error("Please make sure that you're passing a valid element");
            }
            if (button.nodeName.toLowerCase() === 'a') {
                // disable link
                $(button).on('click', preventDefault);
            }

            // DOM element
            this._button = button;

            // DOM element
            this._input = null;
            this._disabled = false;

            this.installBind();
        }
        UploadBinding.prototype.createIframe = function () {
            var id = tui.uuid();
            var iframe = tui.toElement('<iframe src="javascript:false;" name="' + id + '" />');
            iframe.setAttribute('id', id);
            iframe.style.display = 'none';
            document.body.appendChild(iframe);
            var doc = iframe.contentDocument ? iframe.contentDocument : window.frames[iframe.id].document;
            doc.charset = "utf-8";
            return iframe;
        };

        UploadBinding.prototype.createForm = function (iframe) {
            var settings = this._settings;
            var form = tui.toElement('<form method="post" enctype="multipart/form-data" accept-charset="UTF-8"></form>');
            form.setAttribute('accept-charset', 'UTF-8');
            if (settings.action)
                form.setAttribute('action', settings.action);
            form.setAttribute('target', iframe.name);
            form.style.display = 'none';
            document.body.appendChild(form);

            for (var prop in settings.data) {
                if (settings.data.hasOwnProperty(prop)) {
                    var el = document.createElement("input");
                    el.setAttribute('type', 'hidden');
                    el.setAttribute('name', prop);
                    el.setAttribute('value', settings.data[prop]);
                    form.appendChild(el);
                }
            }
            return form;
        };

        UploadBinding.prototype.createInput = function () {
            var _this = this;
            var input = document.createElement("input");
            input.setAttribute('type', 'file');
            if (this._settings.accept)
                input.setAttribute('accept', this._settings.accept);
            input.setAttribute('name', this._settings.name);
            if (this._settings.multiple)
                input.setAttribute('multiple', 'multiple');
            if (tui.ieVer > 0)
                input.title = "";
            else
                input.title = " ";
            $(input).css({
                'position': 'absolute',
                'right': 0,
                'margin': 0,
                'padding': 0,
                'fontSize': '480px',
                'fontFamily': 'sans-serif',
                'cursor': 'pointer'
            });
            var div = document.createElement("div");
            $(div).css({
                'display': 'block',
                'position': 'absolute',
                'overflow': 'hidden',
                'margin': 0,
                'padding': 0,
                'opacity': 0,
                'direction': 'ltr',
                //Max zIndex supported by Opera 9.0-9.2
                'zIndex': 2147483583
            });

            // Make sure that element opacity exists.
            // Otherwise use IE filter
            if (div.style.opacity !== "0") {
                if (typeof (div.filters) === 'undefined') {
                    throw new Error('Opacity not supported by the browser');
                }
                div.style.filter = "alpha(opacity=0)";
            }
            $(input).on('change', function () {
                if (!input || input.value === '') {
                    return;
                }

                // Get filename from input, required
                // as some browsers have path instead of it
                var file = fileFromPath(input.value);
                var fileExt = getExt(file);

                // Check accept mimetype, now we only check by submit event.
                //if (this._settings.accept) {
                //	if (!checkExtWithMimeType(fileExt, this._settings.accept)) {
                //		this.clearInput();
                //		this.fire("invalid", { "file": file, "ext": fileExt });
                //		return;
                //	}
                //}
                if (_this.fire("change", { "file": file, "ext": fileExt }) === false) {
                    _this.clearInput();
                    return;
                }

                // Submit form when value is changed
                if (_this._settings.autoSubmit) {
                    _this.submit();
                }
            });
            $(input).on('mouseover', function () {
                $(_this._button).addClass(_this._settings.hoverClass);
            });
            $(input).on('mouseout', function () {
                $(_this._button).removeClass(_this._settings.hoverClass);
                $(_this._button).removeClass(_this._settings.focusClass);

                if (input.parentNode) {
                    // We use visibility instead of display to fix problem with Safari 4
                    // The problem is that the value of input doesn't change if it
                    // has display none when user selects a file
                    input.parentNode.style.visibility = 'hidden';
                }
            });
            $(input).on('focus', function () {
                $(_this._button).addClass(_this._settings.focusClass);
            });
            $(input).on('blur', function () {
                $(_this._button).removeClass(_this._settings.focusClass);
            });
            div.appendChild(input);
            this._button.offsetParent.appendChild(div);
            this._input = input;
        };

        UploadBinding.prototype.deleteInput = function () {
            if (!this._input) {
                return;
            }
            tui.removeNode(this._input.parentNode);
            this._input = null;
            $(this._button).removeClass(this._settings.hoverClass);
            $(this._button).removeClass(this._settings.focusClass);
        };

        UploadBinding.prototype.clearInput = function () {
            this.deleteInput();
            this.createInput();
        };

        /**
        * Gets response from iframe and fires onComplete event when ready
        * @param iframe
        * @param file Filename to use in onComplete callback
        */
        UploadBinding.prototype.processResponse = function (iframe, file) {
            var _this = this;
            // getting response
            var toDeleteFlag = false, settings = this._settings;
            $(iframe).on('load', function () {
                if (iframe.src === "javascript:'%3Chtml%3E%3C/html%3E';" || iframe.src === "javascript:'<html></html>';") {
                    // First time around, do not delete.
                    // We reload to blank page, so that reloading main page
                    // does not re-submit the post.
                    if (toDeleteFlag) {
                        // Fix busy state in FF3
                        setTimeout(function () {
                            tui.removeNode(iframe);
                        }, 0);
                    }
                    return;
                }

                var doc = iframe.contentDocument ? iframe.contentDocument : window.frames[iframe.id].document;

                // fixing Opera 9.26,10.00
                if (doc.readyState && doc.readyState !== 'complete') {
                    return;
                }

                // fixing Opera 9.64
                if (doc.body && doc.body.innerHTML === "false") {
                    return;
                }
                var response;
                if (doc.XMLDocument) {
                    // response is a xml document Internet Explorer property
                    response = doc.XMLDocument;
                } else if (doc.body) {
                    // response is html document or plain text
                    response = doc.body.innerHTML;
                    if (settings.responseType && settings.responseType.toLowerCase() === 'json') {
                        if (doc.body.firstChild && doc.body.firstChild.nodeName.toUpperCase() === 'PRE') {
                            doc.normalize();
                            response = doc.body.firstChild.firstChild.nodeValue;
                        }
                        if (response) {
                            try  {
                                response = eval("(" + response + ")");
                            } catch (e) {
                                response = null;
                            }
                        } else {
                            response = null;
                        }
                    }
                } else {
                    // response is a xml document
                    response = doc;
                }
                _this.fire("complete", { "file": file, "ext": getExt(file), "response": response });

                // Reload blank page, so that reloading main page
                // does not re-submit the post. Also, remember to
                // delete the frame
                toDeleteFlag = true;

                // Fix IE mixed content issue
                iframe.src = "javascript:'<html></html>';";
                tui.removeNode(iframe);
            });
        };

        UploadBinding.prototype.submit = function (exparams) {
            if (!this._input || this._input.value === '') {
                return;
            }
            var file = fileFromPath(this._input.value);

            // user returned false to cancel upload
            if (this.fire("submit", { "file": file, "ext": getExt(file) }) === false) {
                this.clearInput();
                return;
            }

            // sending request
            var iframe = this.createIframe();
            var form = this.createForm(iframe);

            // assuming following structure
            // div -> input type='file'
            tui.removeNode(this._input.parentNode);
            $(this._button).removeClass(this._settings.hoverClass);
            $(this._button).removeClass(this._settings.focusClass);
            form.appendChild(this._input);
            var el = document.createElement("input");
            el.setAttribute('type', 'hidden');
            el.setAttribute('name', "exparams");
            el.setAttribute('value', exparams);
            form.appendChild(el);
            form.submit();

            // request set, clean up
            tui.removeNode(form);
            form = null;
            this.deleteInput();

            // Get response from iframe and fire onComplete event when ready
            this.processResponse(iframe, file);

            // get ready for next request
            this.createInput();
        };

        UploadBinding.prototype.disabled = function (val) {
            if (typeof val === "boolean") {
                this._disabled = val;
                return this;
            } else
                return this._disabled;
        };

        UploadBinding.prototype.installBind = function () {
            $(this._button).on('mouseover', { self: this }, UploadBinding.makeBind);
        };

        UploadBinding.prototype.uninstallBind = function () {
            this.deleteInput();
            $(this._button).off('mouseover', UploadBinding.makeBind);
        };
        UploadBinding.makeBind = (function (e) {
            var self = e.data.self;
            if (self._disabled) {
                return;
            }
            if (!self._input) {
                self.createInput();
            }
            var div = self._input.parentNode;
            copyLayout(self._button, div);
            div.style.visibility = 'visible';
        });
        return UploadBinding;
    })(tui.EventObject);
    tui.UploadBinding = UploadBinding;

    function bindUpload(button, options) {
        return new UploadBinding(button, options);
    }
    tui.bindUpload = bindUpload;
})(tui || (tui = {}));
/// <reference path="tui.core.ts" />
var tui;
(function (tui) {
    var ArrayProvider = (function () {
        function ArrayProvider(data) {
            this._src = null;
            this._data = null;
            this._head = null;
            this._headCache = {};
            if (data && data instanceof Array) {
                this._src = this._data = data;
            } else if (data && data.data) {
                this._src = this._data = data.data;
                if (data.head)
                    this._head = data.head;
                else
                    this._head = null;
            } else
                throw new Error("TUI Grid: Unsupported data format!");
        }
        ArrayProvider.prototype.length = function () {
            if (this._data)
                return this._data.length;
            else
                return 0;
        };
        ArrayProvider.prototype.at = function (index) {
            if (this._data)
                return this._data[index];
            else
                return null;
        };
        ArrayProvider.prototype.columnKeyMap = function () {
            if (this._head) {
                var map = {};
                for (var i = 0; i < this._head.length; i++) {
                    map[this._head[i]] = i;
                }
                return map;
            } else
                return {};
        };
        ArrayProvider.prototype.sort = function (key, desc, func) {
            if (typeof func === "undefined") { func = null; }
            if (this._src) {
                if (typeof func === "function") {
                    this._data = this._src.concat();
                    this._data.sort(func);
                } else if (key === null && func === null) {
                    this._data = this._src;
                    return this;
                } else {
                    if (this._head && typeof key === "string") {
                        key = this._head.indexOf(key);
                    }
                    this._data = this._src.concat();
                    this._data.sort(function (a, b) {
                        if (a[key] > b[key]) {
                            return desc ? -1 : 1;
                        } else if (a[key] < b[key]) {
                            return desc ? 1 : -1;
                        } else {
                            return 0;
                        }
                    });
                }
            } else {
                this._data = null;
            }
            return this;
        };

        ArrayProvider.prototype.data = function (data) {
            if (data) {
                if (data instanceof Array) {
                    this._src = this._data = data;
                    return this;
                } else if (data.data) {
                    this._src = this._data = data.data;
                    if (data.head)
                        this._head = data.head;
                    else
                        this._head = null;
                    return this;
                } else
                    throw new Error("TUI Grid: Unsupported data format!");
            } else {
                return this._data;
            }
        };

        /**
        * ArrayDataProvider peculiar, get source data set
        */
        ArrayProvider.prototype.src = function () {
            return this._src;
        };

        ArrayProvider.prototype.process = function (func) {
            this._data = func(this._src);
        };
        return ArrayProvider;
    })();
    tui.ArrayProvider = ArrayProvider;

    var RemoteCursorProvider = (function () {
        function RemoteCursorProvider(cacheSize) {
            if (typeof cacheSize === "undefined") { cacheSize = 100; }
            this._queryTimer = null;
            this._firstQuery = true;
            this._cacheSize = cacheSize;
            this._invalid = true;
            this._data = [];
            this._begin = 0;
            this._length = 0;
            this._sortKey = null;
        }
        RemoteCursorProvider.prototype.length = function () {
            if (this._invalid) {
                this.doQuery(0);
            }
            return this._length;
        };

        RemoteCursorProvider.prototype.at = function (index) {
            if (index < 0 || index >= this.length()) {
                return null;
            } else if (this._invalid || index < this._begin || index >= this._begin + this._data.length) {
                this.doQuery(index);
            }
            if (index >= this._begin || index < this._begin + this._data.length)
                return this._data[index - this._begin];
            else
                return null;
        };
        RemoteCursorProvider.prototype.columnKeyMap = function () {
            if (this._head) {
                var map = {};
                for (var i = 0; i < this._head.length; i++) {
                    map[this._head[i]] = i;
                }
                return map;
            } else
                return {};
        };
        RemoteCursorProvider.prototype.sort = function (key, desc, func) {
            if (typeof func === "undefined") { func = null; }
            this._sortKey = key;
            this._desc = desc;
            this._invalid = true;
            return this;
        };

        RemoteCursorProvider.prototype.doQuery = function (begin) {
            var _this = this;
            if (typeof this._queryCallback !== "function") {
                return;
            }
            if (this._queryTimer !== null)
                clearTimeout(this._queryTimer);
            var self = this;
            var cacheBegin = begin - Math.round(this._cacheSize / 2);
            if (cacheBegin < 0)
                cacheBegin = 0;
            var queryInfo = {
                begin: cacheBegin,
                cacheSize: this._cacheSize,
                sortKey: this._sortKey,
                sortDesc: this._desc,
                update: function (info) {
                    self._data = info.data;
                    self._length = info.length;
                    self._begin = info.begin;
                    self._invalid = false;
                    if (typeof info.head !== tui.undef) {
                        self._head = info.head;
                    }
                    if (typeof self._updateCallback === "function") {
                        self._updateCallback({
                            length: self._length,
                            begin: self._begin,
                            data: self._data
                        });
                    }
                }
            };
            if (this._firstQuery) {
                this._firstQuery = false;
                this._queryCallback(queryInfo);
            } else {
                this._queryTimer = setTimeout(function () {
                    _this._firstQuery = true;
                    _this._queryTimer = null;
                    _this._queryCallback(queryInfo);
                }, 100);
            }
        };

        RemoteCursorProvider.prototype.onupdate = function (callback) {
            this._updateCallback = callback;
        };

        // Cursor own functions
        RemoteCursorProvider.prototype.onquery = function (callback) {
            this._queryCallback = callback;
        };
        return RemoteCursorProvider;
    })();
    tui.RemoteCursorProvider = RemoteCursorProvider;
})(tui || (tui = {}));
/// <reference path="tui.core.ts" />
var tui;
(function (tui) {
    var _maskDiv = document.createElement("div");
    _maskDiv.className = "tui-mask";
    _maskDiv.setAttribute("unselectable", "on");
    var mousewheelevt = (/Firefox/i.test(navigator.userAgent)) ? "DOMMouseScroll" : "mousewheel";
    $(_maskDiv).on(mousewheelevt, function (ev) {
        ev.stopPropagation();
        ev.preventDefault();
    });

    var _tooltip = document.createElement("span");
    _tooltip.className = "tui-tooltip";
    _tooltip.setAttribute("unselectable", "on");

    var _tooltipTarget = null;

    /**
    * Show a mask layer to prevent user drag or select document elements which don't want to be affected.
    * It's very useful when user perform a dragging operation.
    */
    function mask() {
        document.body.appendChild(_maskDiv);
        return _maskDiv;
    }
    tui.mask = mask;

    /**
    * Close a mask layer
    */
    function unmask() {
        if (_maskDiv.parentNode)
            _maskDiv.parentNode.removeChild(_maskDiv);
        _maskDiv.innerHTML = "";
        _maskDiv.style.cursor = "";
        return _maskDiv;
    }
    tui.unmask = unmask;

    function showTooltip(target, tooltip) {
        if (target === _tooltipTarget || target === _tooltip)
            return;
        document.body.appendChild(_tooltip);
        _tooltip.innerHTML = tooltip;
        _tooltipTarget = target;
        var pos = tui.fixedPosition(target);
        if (target.offsetWidth < 20)
            _tooltip.style.left = (pos.x + target.offsetWidth / 2 - 17) + "px";
        else
            _tooltip.style.left = pos.x + "px";
        _tooltip.style.top = pos.y + 8 + target.offsetHeight + "px";
    }
    tui.showTooltip = showTooltip;

    function closeTooltip() {
        if (_tooltip.parentNode)
            _tooltip.parentNode.removeChild(_tooltip);
        _tooltip.innerHTML = "";
        _tooltipTarget = null;
    }
    tui.closeTooltip = closeTooltip;

    function whetherShowTooltip(target) {
        if (target === _tooltip)
            return;
        var obj = target;
        while (obj) {
            var tooltip = obj.getAttribute("data-tooltip");
            if (tooltip) {
                showTooltip(obj, tooltip);
                return;
            } else {
                obj = obj.parentElement;
            }
        }
        if (!obj)
            closeTooltip();
    }
    tui.whetherShowTooltip = whetherShowTooltip;

    function whetherCloseTooltip(target) {
        if (target !== _tooltipTarget && target !== _tooltip) {
            closeTooltip();
        }
    }
    tui.whetherCloseTooltip = whetherCloseTooltip;
})(tui || (tui = {}));
var tui;
(function (tui) {
    (function (_ctrl) {
        var Control = (function (_super) {
            __extends(Control, _super);
            function Control(tagName, className, el) {
                _super.call(this);
                this._exposedEvents = {};
                if (typeof el === "object")
                    this.elem(el);
                else
                    this.elem(tagName, className);
                if (this[0])
                    this[0]._ctrl = this;
            }
            /**
            * Construct a component
            */
            Control.prototype.elem = function (el, clsName) {
                if (el && el.nodeName || el === null) {
                    this[0] = el;
                    this.bindMainElementEvent();
                } else if (typeof el === "string" && typeof clsName === "string") {
                    this[0] = document.createElement(el);
                    this[0].className = clsName;
                    this.bindMainElementEvent();
                }
                return this[0];
            };

            Control.prototype.bindMainElementEvent = function () {
                if (!this[0]) {
                    return;
                }
                var self = this;
                $(this[0]).focus(function () {
                    self.addClass("tui-focus");
                });
                $(this[0]).blur(function () {
                    self.removeClass("tui-focus");
                });
            };

            Control.prototype.exposeEvents = function (eventNames) {
                if (this[0]) {
                    if (typeof eventNames === "string")
                        eventNames = eventNames.split(/\s+/);
                    for (var i = 0; i < eventNames.length; i++) {
                        this._exposedEvents[eventNames[i]] = true;
                    }
                }
            };

            Control.prototype.bind = function (eventName, handler, priority) {
                if (this._exposedEvents[eventName]) {
                    $(this[0]).on(eventName, handler);
                } else
                    _super.prototype.bind.call(this, eventName, handler, priority);
            };

            Control.prototype.unbind = function (eventName, handler) {
                if (this._exposedEvents[eventName]) {
                    $(this[0]).off(eventName, handler);
                } else
                    _super.prototype.unbind.call(this, eventName, handler);
            };

            Control.prototype.id = function (val) {
                if (typeof val === "string") {
                    if (this[0])
                        this[0].id = val;
                    return this;
                } else {
                    if (this[0] && this[0].id)
                        return this[0].id;
                    else
                        return null;
                }
            };

            Control.prototype.hasAttr = function (attributeName) {
                if (this[0])
                    return typeof $(this[0]).attr(attributeName) === "string";
                else
                    return false;
            };
            Control.prototype.isAttrTrue = function (attributeName) {
                if (this.hasAttr(attributeName)) {
                    var attr = this.attr(attributeName).toLowerCase();
                    return attr === "" || attr === "true" || attr === "on";
                } else
                    return false;
            };

            Control.prototype.attr = function (p1, p2) {
                if (typeof p1 === "string" && typeof p2 === tui.undef) {
                    if (!this[0])
                        return null;
                    else {
                        var val = $(this[0]).attr(p1);
                        if (val === null || typeof val === tui.undef)
                            return null;
                        else
                            return val;
                    }
                } else {
                    if (this[0])
                        $(this[0]).attr(p1, p2);
                    return this;
                }
            };

            Control.prototype.removeAttr = function (attributeName) {
                if (this[0])
                    $(this[0]).removeAttr(attributeName);
                return this;
            };

            Control.prototype.css = function (p1, p2) {
                if (typeof p1 === "string" && typeof p2 === tui.undef) {
                    if (!this[0])
                        return null;
                    else
                        return $(this[0]).css(p1);
                } else {
                    if (this[0])
                        $(this[0]).css(p1, p2);
                    return this;
                }
            };

            Control.prototype.hasClass = function (className) {
                if (this[0])
                    return $(this[0]).hasClass(className);
                else
                    return false;
            };

            Control.prototype.addClass = function (param) {
                if (this[0])
                    $(this[0]).addClass(param);
                return this;
            };

            Control.prototype.removeClass = function (param) {
                if (this[0])
                    $(this[0]).removeClass(param);
                return this;
            };
            Control.prototype.refresh = function () {
            };

            Control.prototype.is = function (attrName, val) {
                if (typeof val === "boolean") {
                    if (val)
                        this.attr(attrName, "true");
                    else
                        this.removeAttr(attrName);
                    if (this[0] && tui.ieVer > 0 && tui.ieVer <= 8) {
                        this[0].className = this[0].className;
                    }
                    return this;
                } else {
                    return this.isAttrTrue(attrName);
                }
            };

            Control.prototype.hidden = function (val) {
                if (typeof val === "boolean") {
                    this.is("data-hidden", val);
                    if (val) {
                        this.addClass("tui-hidden");
                    } else
                        this.removeClass("tui-hidden");
                    return this;
                } else
                    return this.is("data-hidden");
            };

            Control.prototype.checked = function (val) {
                if (typeof val === "boolean") {
                    this.is("data-checked", val);
                    this.fire("check", { ctrl: this, checked: val });
                    return this;
                } else
                    return this.is("data-checked");
            };

            Control.prototype.actived = function (val) {
                if (typeof val === "boolean") {
                    this.is("data-actived", val);
                    if (val) {
                        this.addClass("tui-actived");
                    } else
                        this.removeClass("tui-actived");
                    return this;
                } else
                    return this.is("data-actived");
            };

            Control.prototype.disabled = function (val) {
                return this.is("data-disabled", val);
            };

            Control.prototype.marked = function (val) {
                return this.is("data-marked", val);
            };

            Control.prototype.selectable = function (val) {
                if (typeof val === "boolean") {
                    if (!val)
                        this.attr("unselectable", "on");
                    else
                        this.removeAttr("unselectable");
                    return this;
                } else {
                    return !this.isAttrTrue("unselectable");
                }
            };

            Control.prototype.ajaxForm = function (txt) {
                if (typeof txt === "string") {
                    this.attr("data-ajax-form", txt);
                    return this;
                } else
                    return this.attr("data-ajax-form");
            };

            Control.prototype.ajaxField = function (txt) {
                if (typeof txt === "string") {
                    this.attr("data-ajax-field", txt);
                    return this;
                } else
                    return this.attr("data-ajax-field");
            };

            Control.prototype.blur = function () {
                var el = this.elem();
                if (el) {
                    el.blur();
                }
            };
            Control.prototype.focus = function () {
                var el = this.elem();
                if (el) {
                    setTimeout(function () {
                        el.focus();
                    }, 0);
                }
            };

            Control.prototype.isHover = function () {
                if (this[0]) {
                    return tui.isAncestry(_hoverElement, this[0]);
                } else
                    return false;
            };

            Control.prototype.isFocused = function () {
                if (this[0]) {
                    return tui.isAncestry(document.activeElement, this[0]);
                } else
                    return false;
            };

            Control.prototype.isAncestry = function (ancestry) {
                return tui.isAncestry(this[0], ancestry);
            };

            Control.prototype.isPosterity = function (posterity) {
                return tui.isPosterity(this[0], posterity);
            };
            return Control;
        })(tui.EventObject);
        _ctrl.Control = Control;

        function control(param, constructor, constructParam) {
            var elem = null;
            if (typeof param === "string" && param) {
                elem = document.getElementById(param);
                if (!elem)
                    return null;
                if (elem._ctrl) {
                    elem._ctrl.refresh();
                    return elem._ctrl;
                } else if (typeof constructParam !== tui.undef) {
                    return new constructor(elem, constructParam);
                } else
                    return new constructor(elem);
            } else if (param && param.nodeName) {
                elem = param;
                if (elem._ctrl) {
                    elem._ctrl.refresh();
                    return elem._ctrl;
                } else if (typeof constructParam !== tui.undef) {
                    return new constructor(elem, constructParam);
                } else
                    return new constructor(elem);
            } else if ((typeof param === tui.undef || param === null) && constructor) {
                if (typeof constructParam !== tui.undef) {
                    return new constructor(null, constructParam);
                } else
                    return new constructor();
            } else
                return null;
        }
        _ctrl.control = control;

        var initializers = {};
        function registerInitCallback(clsName, constructFunc) {
            if (!initializers[clsName]) {
                initializers[clsName] = constructFunc;
            }
        }
        _ctrl.registerInitCallback = registerInitCallback;

        function initCtrls(parent) {
            for (var clsName in initializers) {
                if (clsName) {
                    var func = initializers[clsName];
                    $(parent).find("." + clsName).each(function (idx, elem) {
                        func(elem);
                    });
                }
            }
        }
        _ctrl.initCtrls = initCtrls;

        var checkTooltipTimeout = null;
        var _hoverElement;
        $(window.document).mousemove(function (e) {
            _hoverElement = e.target || e.toElement;

            if (e.button === 0 && (e.which === 1 || e.which === 0)) {
                if (checkTooltipTimeout)
                    clearTimeout(checkTooltipTimeout);
                checkTooltipTimeout = setTimeout(function () {
                    tui.whetherShowTooltip(_hoverElement);
                }, 20);
            }
        });
        $(window).scroll(function () {
            tui.closeTooltip();
        });

        $(window.document).ready(function () {
            initCtrls(document);
            tui.fire("initialized", null);
        });
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;
})(tui || (tui = {}));
var tui;
(function (tui) {
    /// <reference path="tui.ctrl.control.ts" />
    (function (_ctrl) {
        var FormAgent = (function (_super) {
            __extends(FormAgent, _super);
            function FormAgent(el) {
                _super.call(this, "span", FormAgent.CLASS, el);
                var parent = this[0].parentElement;
                while (parent) {
                    if ($(parent).hasClass("tui-form")) {
                        this.ajaxForm($(parent).attr("id"));
                        break;
                    }
                }
                if (!this.hasAttr("data-target-property")) {
                    this.targetProperty("value");
                }
            }
            FormAgent.prototype.validate = function () {
                var param = { valid: true };
                if (this.fire("validate", param) === false)
                    return param.valid;
                var target = this.target();
                var isGroup = this.isGroup();
                if (!target)
                    return true;
                if (isGroup) {
                    var validator = this.groupValidator();
                    if (!validator)
                        return true;
                    var controls = $("." + _ctrl.Radiobox.CLASS + "[data-group='" + target + "'],." + _ctrl.Checkbox.CLASS + "[data-group='" + target + "']");
                    var values = [];
                    controls.each(function (index, elem) {
                        if (tui.parseBoolean($(elem).attr("data-checked")))
                            values.push($(elem).attr("data-value"));
                    });
                    var valid = true;
                    for (var k in validator) {
                        if (k && validator.hasOwnProperty(k)) {
                            if (k.substr(0, 5) === "*max:") {
                                var imax = parseFloat(k.substr(5));
                                if (isNaN(imax))
                                    throw new Error("Invalid validator: '*max:...' must follow a number");
                                var ival = values.length;
                                if (ival > imax) {
                                    valid = false;
                                }
                            } else if (k.substr(0, 5) === "*min:") {
                                var imin = parseFloat(k.substr(5));
                                if (isNaN(imin))
                                    throw new Error("Invalid validator: '*min:...' must follow a number");
                                var ival = values.length;
                                if (ival < imin) {
                                    valid = false;
                                }
                            } else {
                                valid = values.indexOf(k) >= 0;
                            }
                            if (!valid) {
                                controls.each(function (index, elem) {
                                    var ctrl = elem["_ctrl"];
                                    if (ctrl && typeof ctrl.notify === "function")
                                        ctrl.notify(validator[k]);
                                });
                                break;
                            }
                        }
                    }
                    return valid;
                } else {
                    var elem = document.getElementById(target);
                    if (elem && elem["_ctrl"]) {
                        var ctrl = elem["_ctrl"];
                        if (typeof ctrl.validate === "function") {
                            return ctrl.validate();
                        }
                    }
                    return true;
                }
            };

            FormAgent.prototype.target = function (val) {
                if (typeof val !== tui.undef) {
                    this.attr("data-target", val);
                    return this;
                } else
                    return this.attr("data-target");
            };

            FormAgent.prototype.targetProperty = function (val) {
                if (typeof val !== tui.undef) {
                    this.attr("data-target-property", val);
                    return this;
                } else
                    return this.attr("data-target-property");
            };

            FormAgent.prototype.groupValidator = function (val) {
                if (typeof val === "object" && val) {
                    this.attr("data-group-validator", JSON.stringify(val));
                    return this;
                } else if (val === null) {
                    this.removeAttr("data-group-validator");
                    return this;
                } else {
                    var strval = this.attr("data-group-validator");
                    if (strval === null) {
                        return null;
                    } else {
                        try  {
                            val = eval("(" + strval + ")");
                            if (typeof val !== "object")
                                return null;
                            else
                                return val;
                        } catch (err) {
                            return null;
                        }
                    }
                }
            };

            FormAgent.prototype.isGroup = function (val) {
                if (typeof val !== tui.undef) {
                    this.is("data-is-group", !!val);
                    return this;
                } else
                    return this.is("data-is-group");
            };

            FormAgent.prototype.value = function (val) {
                var property = this.targetProperty();
                var target = this.target();
                var isGroup = this.isGroup();
                if (typeof val !== tui.undef) {
                    var param = { value: val };
                    if (this.fire("setvalue", param) === false)
                        return this;
                    if (!target) {
                        this.attr("data-value", JSON.stringify(val));
                        return this;
                    }
                    if (isGroup) {
                        var controls = $("." + _ctrl.Radiobox.CLASS + "[data-group='" + target + "'],." + _ctrl.Checkbox.CLASS + "[data-group='" + target + "']");
                        var values;
                        if (val && typeof val.length === "number")
                            values = val;
                        else if (val === null)
                            values = [];
                        else
                            values = [val];

                        controls.each(function (index, elem) {
                            var ctrl = elem["_ctrl"];
                            if (typeof ctrl[property] === "function") {
                                if (values.indexOf(ctrl[property]()) >= 0) {
                                    ctrl.checked(true);
                                } else
                                    ctrl.checked(false);
                            }
                        });
                    } else {
                        var elem = document.getElementById(target);
                        if (elem && elem["_ctrl"]) {
                            var ctrl = elem["_ctrl"];
                            if (typeof ctrl[property] === "function") {
                                ctrl[property](val);
                            }
                        } else if (elem) {
                            if (typeof elem[property] === "function") {
                                elem[property](val);
                            } else {
                                elem[property] = val;
                            }
                        }
                    }
                    return this;
                } else {
                    if (!target) {
                        var strval = this.attr("data-value");
                        if (strval === null) {
                            return null;
                        } else {
                            try  {
                                return eval("(" + strval + ")");
                            } catch (err) {
                                return null;
                            }
                        }
                    }
                    var param = { value: null };
                    if (this.fire("getvalue", param) === false)
                        return param.value;
                    if (isGroup) {
                        var controls = $("." + _ctrl.Radiobox.CLASS + "[data-group='" + target + "']");
                        var values = [];
                        if (controls.length > 0) {
                            controls.each(function (index, elem) {
                                var ctrl = elem["_ctrl"];
                                if (ctrl) {
                                    if (typeof ctrl.checked === "function" && ctrl.checked() && typeof ctrl[property] === "function") {
                                        values.push(ctrl[property]());
                                    }
                                }
                            });
                            if (values.length > 0)
                                return values[0];
                            else
                                return null;
                        } else {
                            controls = $("." + _ctrl.Checkbox.CLASS + "[data-group='" + target + "']");
                            controls.each(function (index, elem) {
                                var ctrl = elem["_ctrl"];
                                if (ctrl) {
                                    if (typeof ctrl.checked === "function" && ctrl.checked() && typeof ctrl[property] === "function") {
                                        values.push(ctrl[property]());
                                    }
                                }
                            });
                            return values;
                        }
                    } else {
                        var elem = document.getElementById(target);
                        if (elem && elem["_ctrl"]) {
                            var ctrl = elem["_ctrl"];
                            if (typeof ctrl[property] === "function") {
                                return ctrl[property]();
                            }
                        } else if (elem) {
                            if (typeof elem[property] === "function") {
                                return elem[property]();
                            } else {
                                return elem[property];
                            }
                        }
                        return null;
                    }
                }
            };
            FormAgent.CLASS = "tui-form-agent";
            return FormAgent;
        })(_ctrl.Control);
        _ctrl.FormAgent = FormAgent;

        function formAgent(param) {
            return tui.ctrl.control(param, FormAgent);
        }
        _ctrl.formAgent = formAgent;
        tui.ctrl.registerInitCallback(FormAgent.CLASS, formAgent);
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;
})(tui || (tui = {}));
var tui;
(function (tui) {
    /// <reference path="tui.ctrl.formagent.ts" />
    (function (_ctrl) {
        var Form = (function (_super) {
            __extends(Form, _super);
            function Form(el) {
                _super.call(this, "span", Form.CLASS, el);

                if (!this.hasAttr("data-method")) {
                    this.method("POST");
                }
                if (!this.hasAttr("data-timeout")) {
                    this.timeout(60000);
                }
                if (!this.hasAttr("data-target-property")) {
                    this.targetProperty("value");
                }
                if (!this.hasAttr("data-show-error")) {
                    this.isShowError(true);
                }

                if (this.id() === null)
                    this.id(tui.uuid());

                for (var i = 0; i < this[0].childNodes.length; i++) {
                    if (this[0].childNodes[i].nodeName.toLowerCase() === "span") {
                        var agent = tui.ctrl.formAgent(this[0].childNodes[i]);
                        agent.ajaxForm(this.id());
                    }
                }

                var self = this;
                if (this.isAutoSubmit()) {
                    tui.on("initialized", function () {
                        self.submit();
                    });
                }
            }
            Form.prototype.isAutoSubmit = function (val) {
                if (typeof val !== tui.undef) {
                    this.is("data-auto-submit", !!val);
                    return this;
                } else
                    return this.is("data-auto-submit");
            };

            Form.prototype.isShowError = function (val) {
                if (typeof val !== tui.undef) {
                    this.is("data-show-error", !!val);
                    return this;
                } else
                    return this.is("data-show-error");
            };

            Form.prototype.action = function (url) {
                if (typeof url === "string") {
                    this.attr("data-action", url);
                    return this;
                } else
                    return this.attr("data-action");
            };

            Form.prototype.method = function (val) {
                if (typeof val === "string" && Form.METHODS.indexOf(val.toUpperCase()) >= 0) {
                    this.attr("data-method", val.toUpperCase());
                    return this;
                } else
                    return this.attr("data-method").toUpperCase();
            };

            Form.prototype.timeout = function (val) {
                if (typeof val === "number") {
                    this.attr("data-timeout", Math.round(val) + "");
                    return this;
                } else
                    return parseInt(this.attr("data-timeout"), 10);
            };

            Form.prototype.target = function (val) {
                if (typeof val === "string") {
                    this.attr("data-target", val);
                    return this;
                } else
                    return this.attr("data-target");
            };

            Form.prototype.targetProperty = function (val) {
                if (typeof val === "string") {
                    this.attr("data-target-property", val);
                    return this;
                } else
                    return this.attr("data-target-property");
            };

            Form.prototype.targetRedirect = function (val) {
                if (typeof val === "string") {
                    this.attr("data-target-redirect", val);
                    return this;
                } else
                    return this.attr("data-target-redirect");
            };

            Form.prototype.targetSubmitForm = function (val) {
                if (typeof val === "string") {
                    this.attr("data-target-submit-form", val);
                    return this;
                } else
                    return this.attr("data-target-submit-form");
            };

            Form.prototype.validate = function () {
                var id = this.id();
                if (!id) {
                    return true;
                }
                var valid = true;
                $("[data-ajax-form='" + id + "']").each(function (index, elem) {
                    if (typeof this._ctrl.validate === "function")
                        if (!this._ctrl.validate())
                            valid = false;
                });
                return valid;
            };

            Form.prototype.immediateValue = function (val) {
                if (typeof val !== tui.undef) {
                    this._immediateValue = val;
                    return this;
                } else
                    return this._immediateValue;
            };

            Form.prototype.value = function (val) {
                if (typeof val !== tui.undef) {
                    // Dispatch data to other controls
                    var id = this.id();
                    id && $("[data-ajax-form='" + id + "']").each(function (index, elem) {
                        var field;
                        if (this._ctrl) {
                            field = this._ctrl.ajaxField();
                            if (!field) {
                                return;
                            } else if (field === "*") {
                                if (typeof this._ctrl.value === "function")
                                    this._ctrl.value(val);
                            } else {
                                if (typeof this._ctrl.value === "function" && typeof val[field] !== tui.undef) {
                                    this._ctrl.value(val[field]);
                                }
                            }
                        } else {
                            field = $(elem).attr("data-ajax-field");
                            if (!field) {
                                return;
                            } else if (field === "*") {
                                $(elem).attr("data-value", JSON.stringify(val));
                            } else {
                                if (typeof val[field] !== tui.undef) {
                                    $(elem).attr("data-value", JSON.stringify(val[field]));
                                }
                            }
                        }
                    });
                    return this;
                } else {
                    var result = {};

                    // Collect all fields from other controls
                    var id = this.id();
                    id && $("[data-ajax-form='" + id + "']").each(function (index, elem) {
                        var field;
                        var val;
                        if (this._ctrl) {
                            field = this._ctrl.ajaxField();
                            if (!field)
                                return;
                            if (this._ctrl.value)
                                val = this._ctrl.value();
                            else
                                return;
                        } else {
                            field = $(elem).attr("data-ajax-field");
                            if (typeof field !== "string")
                                return;
                            val = $(elem).attr("data-value");
                            if (typeof val !== "string")
                                return;
                            try  {
                                val = JSON.parse(val);
                            } catch (e) {
                            }
                        }
                        if (field === "*")
                            result = val;
                        else if (result)
                            result[field] = val;
                    });
                    return result;
                }
            };

            Form.prototype.clear = function () {
                this._immediateValue = tui.undefVal;
                var id = this.id();
                id && $("[data-ajax-form='" + id + "']").each(function (index, elem) {
                    if (elem._ctrl) {
                        if (typeof elem._ctrl.value === "function")
                            elem._ctrl.value(null);
                    } else {
                        $(elem).attr("data-value", "");
                        $(elem).removeAttr("data-value");
                    }
                });
            };

            Form.prototype.submit = function () {
                if (!this.validate())
                    return;
                var action = this.action();
                if (!action)
                    return;
                var id = this.id();
                if (!id)
                    return;
                var data = this.immediateValue();
                if (typeof data === tui.undef)
                    data = this.value();
                if (this.fire("submit", { id: this.id(), data: data }) === false)
                    return;
                var self = this;
                $.ajax({
                    "type": this.method(),
                    "timeout": this.timeout(),
                    "url": action,
                    "contentType": "application/json",
                    "data": (this.method() === "GET" ? data : JSON.stringify(data)),
                    "complete": function (jqXHR, status) {
                        if (status === "success") {
                            if (self.fire("success", { jqXHR: jqXHR, status: status }) === false) {
                                return;
                            }
                        } else {
                            if (self.fire("error", { jqXHR: jqXHR, status: status }) === false) {
                                return;
                            }
                        }
                        if (self.fire("complete", { jqXHR: jqXHR, status: status }) === false) {
                            return;
                        }
                        if (status === "success") {
                            var targetRedirect = self.targetRedirect();
                            if (targetRedirect) {
                                window.location.assign(targetRedirect);
                                return;
                            }
                            var target = self.target();
                            var property = self.targetProperty();
                            if (target) {
                                target = document.getElementById(target);
                                if (target && target["_ctrl"]) {
                                    var ctrl = target["_ctrl"];
                                    if (typeof ctrl[property] === "function") {
                                        ctrl[property](jqXHR["responseJSON"]);
                                    }
                                } else if (target) {
                                    if (typeof target[property] === "function") {
                                        target[property](jqXHR.responseText);
                                    } else {
                                        target[property] = jqXHR.responseText;
                                    }
                                }
                            }
                            var targetSubmitForm = self.targetSubmitForm();
                            if (targetSubmitForm) {
                                var form = tui.ctrl.form(targetSubmitForm);
                                form && form.submit();
                            }
                        } else {
                            if (self.isShowError())
                                tui.errbox(tui.str(status) + " (" + jqXHR.status + ")", tui.str("Failed"));
                        }
                    },
                    "processData": (this.method() === "GET" ? true : false)
                });
            };
            Form.CLASS = "tui-form";
            Form.METHODS = ["GET", "POST", "PUT", "DELETE"];
            Form.STATUS = [
                "success", "notmodified", "error", "timeout", "abort", "parsererror"
            ];
            return Form;
        })(_ctrl.Control);
        _ctrl.Form = Form;

        function form(param) {
            return tui.ctrl.control(param, Form);
        }
        _ctrl.form = form;
        tui.ctrl.registerInitCallback(Form.CLASS, form);
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;
})(tui || (tui = {}));
var tui;
(function (tui) {
    /// <reference path="tui.ctrl.control.ts" />
    /// <reference path="tui.ctrl.form.ts" />
    (function (_ctrl) {
        var Button = (function (_super) {
            __extends(Button, _super);
            function Button(el) {
                var _this = this;
                _super.call(this, "a", Button.CLASS, el);

                this.disabled(this.disabled());
                this.selectable(false);
                this.exposeEvents("mousedown mouseup mousemove mouseenter mouseleave");

                $(this[0]).on("click", function (e) {
                    if (_this.disabled())
                        return;
                    if (_this.fire("click", { "ctrl": _this[0], "event": e }) === false)
                        return;

                    if (tui.fire(_this.id(), { "ctrl": _this[0], "event": e }) === false)
                        return;

                    var formId = _this.submitForm();
                    if (formId) {
                        var form = tui.ctrl.form(formId);
                        form && form.submit();
                    }
                });

                $(this[0]).on("mousedown", function (e) {
                    if (_this.disabled())
                        return;
                    _this.actived(true);
                    var self = _this;
                    function releaseMouse(e) {
                        self.actived(false);
                        $(document).off("mouseup", releaseMouse);
                    }
                    $(document).on("mouseup", releaseMouse);
                });

                $(this[0]).on("keydown", function (e) {
                    if (_this.disabled())
                        return;
                    var isButton = _this[0].nodeName.toLowerCase() === "button";
                    if (e.keyCode === 32) {
                        _this.actived(true);
                        if (!isButton)
                            e.preventDefault();
                    }
                    _this.fire("keydown", { "ctrl": _this[0], "event": e });
                    if (e.keyCode === 13 && !isButton) {
                        e.preventDefault();
                        e.type = "click";
                        _this.fire("click", { "ctrl": _this[0], "event": e });
                        tui.fire(_this.id(), { "ctrl": _this[0], "event": e });
                    }
                });

                $(this[0]).on("keyup", function (e) {
                    if (_this.disabled())
                        return;
                    var isButton = _this[0].nodeName.toLowerCase() === "button";
                    if (e.keyCode === 32) {
                        _this.actived(false);
                    }
                    _this.fire("keyup", { "ctrl": _this[0], "event": e });
                    if (e.keyCode === 32 && !isButton) {
                        e.type = "click";
                        _this.fire("click", { "ctrl": _this[0], "event": e });
                        tui.fire(_this.id(), { "ctrl": _this[0], "event": e });
                    }
                });
            }
            Button.prototype.submitForm = function (formId) {
                if (typeof formId === "string") {
                    this.attr("data-submit-form", formId);
                    return this;
                } else
                    return this.attr("data-submit-form");
            };

            Button.prototype.text = function (t) {
                if (this[0])
                    return tui.elementText(this[0], t);
                return null;
            };

            Button.prototype.html = function (t) {
                if (this[0]) {
                    if (typeof t !== tui.undef) {
                        $(this[0]).html(t);
                        return this;
                    } else
                        return $(this[0]).html();
                }
                return null;
            };

            Button.prototype.disabled = function (val) {
                var result = this.is("data-disabled", val);
                if (typeof val === "boolean") {
                    if (val)
                        this.removeAttr("tabIndex");
                    else
                        this.attr("tabIndex", "0");
                }
                return result;
            };
            Button.CLASS = "tui-button";
            return Button;
        })(_ctrl.Control);
        _ctrl.Button = Button;

        /**
        * Construct a button.
        * @param el {HTMLElement or element id or construct info}
        */
        function button(param) {
            return tui.ctrl.control(param, Button);
        }
        _ctrl.button = button;

        tui.ctrl.registerInitCallback(Button.CLASS, button);
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;
})(tui || (tui = {}));
var tui;
(function (tui) {
    /// <reference path="tui.ctrl.button.ts" />
    (function (_ctrl) {
        var Checkbox = (function (_super) {
            __extends(Checkbox, _super);
            function Checkbox(el) {
                var _this = this;
                _super.call(this, "a", Checkbox.CLASS, el);

                this.disabled(this.disabled());

                this.exposeEvents("mousedown mouseup mousemove mouseenter mouseleave");
                $(this[0]).on("click", function (e) {
                    if (_this.disabled())
                        return;
                    if (_this.fire("click", { "ctrl": _this[0], "event": e }) === false)
                        return;
                    if (tui.fire(_this.id(), { "ctrl": _this[0], "event": e }) === false)
                        return;
                    var formId = _this.submitForm();
                    if (formId) {
                        var form = tui.ctrl.form(formId);
                        form && form.submit();
                    }
                });
                $(this[0]).on("mousedown", function (e) {
                    if (_this.disabled())
                        return;
                    if (tui.ffVer > 0)
                        _this.focus();
                });
                $(this[0]).on("mouseup", function (e) {
                    if (_this.disabled())
                        return;
                    _this.checked(!_this.checked());
                });
                $(this[0]).on("keydown", function (e) {
                    if (_this.disabled())
                        return;
                    var isButton = _this[0].nodeName.toLowerCase() === "button";
                    if (e.keyCode === 32) {
                        _this.actived(true);
                        if (!isButton)
                            e.preventDefault();
                    }
                    _this.fire("keydown", { "ctrl": _this[0], "event": e });
                    if (e.keyCode === 13 && !isButton) {
                        e.preventDefault();
                        e.type = "click";
                        _this.checked(!_this.checked());
                        _this.fire("click", { "ctrl": _this[0], "event": e });
                        tui.fire(_this.id(), { "ctrl": _this[0], "event": e });
                    }
                });

                $(this[0]).on("keyup", function (e) {
                    if (_this.disabled())
                        return;
                    var isButton = _this[0].nodeName.toLowerCase() === "button";
                    if (e.keyCode === 32) {
                        _this.actived(false);
                        _this.checked(!_this.checked());
                    }
                    _this.fire("keyup", { "ctrl": _this[0], "event": e });
                    if (e.keyCode === 32 && !isButton) {
                        e.type = "click";
                        _this.fire("click", { "ctrl": _this[0], "event": e });
                        tui.fire(_this.id(), { "ctrl": _this[0], "event": e });
                    }
                });
            }
            Checkbox.prototype.checked = function (val) {
                if (typeof val === tui.undef) {
                    return _super.prototype.checked.call(this);
                } else {
                    _super.prototype.checked.call(this, !!val);
                    this.unNotifyGroup();
                    return this;
                }
            };

            Checkbox.prototype.text = function (val) {
                if (typeof val !== tui.undef) {
                    $(this[0]).html(val);
                    return this;
                } else
                    return $(this[0]).html();
            };

            Checkbox.prototype.group = function (val) {
                if (typeof val !== tui.undef) {
                    this.attr("data-group", val);
                    return this;
                } else
                    return this.attr("data-group");
            };

            Checkbox.prototype.value = function (val) {
                if (typeof val !== tui.undef) {
                    this.attr("data-value", JSON.stringify(val));
                    return this;
                } else {
                    val = this.attr("data-value");
                    if (val === null) {
                        return null;
                    } else {
                        try  {
                            return eval("(" + val + ")");
                        } catch (err) {
                            return null;
                        }
                    }
                }
            };

            Checkbox.prototype.unNotifyGroup = function () {
                var groupName = this.group();
                if (groupName) {
                    $("." + Checkbox.CLASS + "[data-group='" + groupName + "']").each(function (index, elem) {
                        var ctrl = elem["_ctrl"];
                        if (ctrl && typeof ctrl.notify === "function") {
                            ctrl.notify(null);
                        }
                    });
                }
            };

            Checkbox.prototype.notify = function (message) {
                if (typeof message === "string") {
                    this.attr("data-tooltip", message);
                    this.addClass("tui-notify");
                } else if (message === null) {
                    this.attr("data-tooltip", "");
                    this.removeAttr("data-tooltip");
                    this.removeClass("tui-notify");
                }
            };

            Checkbox.prototype.disabled = function (val) {
                var result = this.is("data-disabled", val);
                if (typeof val === "boolean") {
                    if (val)
                        this.removeAttr("tabIndex");
                    else
                        this.attr("tabIndex", "0");
                }
                return result;
            };

            Checkbox.prototype.submitForm = function (formId) {
                if (typeof formId === "string") {
                    this.attr("data-submit-form", formId);
                    return this;
                } else
                    return this.attr("data-submit-form");
            };
            Checkbox.CLASS = "tui-checkbox";
            return Checkbox;
        })(_ctrl.Control);
        _ctrl.Checkbox = Checkbox;

        /**
        * Construct a button.
        * @param el {HTMLElement or element id or construct info}
        */
        function checkbox(param) {
            return tui.ctrl.control(param, Checkbox);
        }
        _ctrl.checkbox = checkbox;
        tui.ctrl.registerInitCallback(Checkbox.CLASS, checkbox);
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;
})(tui || (tui = {}));
var tui;
(function (tui) {
    /// <reference path="tui.ctrl.button.ts" />
    /// <reference path="tui.ctrl.checkbox.ts" />
    (function (_ctrl) {
        var Radiobox = (function (_super) {
            __extends(Radiobox, _super);
            function Radiobox(el) {
                var _this = this;
                _super.call(this, "a", Radiobox.CLASS, el);

                this.disabled(this.disabled());
                this.exposeEvents("mousedown mouseup mousemove mouseenter mouseleave");

                $(this[0]).on("click", function (e) {
                    if (_this.disabled())
                        return;
                    if (_this.fire("click", { "ctrl": _this[0], "event": e }) === false)
                        return;
                    if (tui.fire(_this.id(), { "ctrl": _this[0], "event": e }) === false)
                        return;
                    var formId = _this.submitForm();
                    if (formId) {
                        var form = tui.ctrl.form(formId);
                        form && form.submit();
                    }
                });
                $(this[0]).on("mousedown", function (e) {
                    if (_this.disabled())
                        return;
                    if (tui.ffVer > 0)
                        _this.focus();
                });
                $(this[0]).on("mouseup", function (e) {
                    if (_this.disabled())
                        return;
                    _this.checked(true);
                });
                $(this[0]).on("keydown", function (e) {
                    if (_this.disabled())
                        return;
                    var isButton = _this[0].nodeName.toLowerCase() === "button";
                    if (e.keyCode === 32) {
                        _this.actived(true);
                        if (!isButton)
                            e.preventDefault();
                    }
                    _this.fire("keydown", { "ctrl": _this[0], "event": e });
                    if (e.keyCode === 13 && !isButton) {
                        e.preventDefault();
                        e.type = "click";
                        _this.checked(true);
                        _this.fire("click", { "ctrl": _this[0], "event": e });
                        tui.fire(_this.id(), { "ctrl": _this[0], "event": e });
                    }
                });

                $(this[0]).on("keyup", function (e) {
                    if (_this.disabled())
                        return;
                    var isButton = _this[0].nodeName.toLowerCase() === "button";
                    if (e.keyCode === 32) {
                        _this.actived(false);
                        _this.checked(true);
                    }
                    _this.fire("keyup", { "ctrl": _this[0], "event": e });
                    if (e.keyCode === 32 && !isButton) {
                        e.type = "click";
                        _this.fire("click", { "ctrl": _this[0], "event": e });
                        tui.fire(_this.id(), { "ctrl": _this[0], "event": e });
                    }
                });
            }
            Radiobox.prototype.text = function (val) {
                if (typeof val !== tui.undef) {
                    $(this[0]).html(val);
                    return this;
                } else
                    return $(this[0]).html();
            };

            Radiobox.prototype.checked = function (val) {
                if (typeof val === tui.undef) {
                    return _super.prototype.checked.call(this);
                } else {
                    val = (!!val);
                    if (val) {
                        var groupName = this.group();
                        if (groupName) {
                            $("." + Radiobox.CLASS + "[data-group='" + groupName + "']").each(function (index, elem) {
                                var ctrl = elem["_ctrl"];
                                if (ctrl && typeof ctrl.checked === "function") {
                                    ctrl.checked(false);
                                }
                            });
                        }
                    }
                    _super.prototype.checked.call(this, val);
                    this.unNotifyGroup();
                    return this;
                }
            };

            Radiobox.prototype.group = function (val) {
                if (typeof val !== tui.undef) {
                    this.attr("data-group", val);
                    return this;
                } else
                    return this.attr("data-group");
            };

            Radiobox.prototype.value = function (val) {
                if (typeof val !== tui.undef) {
                    this.attr("data-value", JSON.stringify(val));
                    return this;
                } else {
                    val = this.attr("data-value");
                    if (val === null) {
                        return null;
                    } else {
                        try  {
                            return eval("(" + val + ")");
                        } catch (err) {
                            return null;
                        }
                    }
                }
            };

            Radiobox.prototype.unNotifyGroup = function () {
                var groupName = this.group();
                if (groupName) {
                    $("." + Radiobox.CLASS + "[data-group='" + groupName + "']").each(function (index, elem) {
                        var ctrl = elem["_ctrl"];
                        if (ctrl && typeof ctrl.notify === "function") {
                            ctrl.notify(null);
                        }
                    });
                }
            };

            Radiobox.prototype.notify = function (message) {
                if (typeof message === "string") {
                    this.attr("data-tooltip", message);
                    this.addClass("tui-notify");
                } else if (message === null) {
                    this.attr("data-tooltip", "");
                    this.removeAttr("data-tooltip");
                    this.removeClass("tui-notify");
                }
            };

            Radiobox.prototype.disabled = function (val) {
                var result = this.is("data-disabled", val);
                if (typeof val === "boolean") {
                    if (val)
                        this.removeAttr("tabIndex");
                    else
                        this.attr("tabIndex", "0");
                }
                return result;
            };

            Radiobox.prototype.submitForm = function (formId) {
                if (typeof formId === "string") {
                    this.attr("data-submit-form", formId);
                    return this;
                } else
                    return this.attr("data-submit-form");
            };
            Radiobox.CLASS = "tui-radiobox";
            return Radiobox;
        })(_ctrl.Control);
        _ctrl.Radiobox = Radiobox;

        /**
        * Construct a button.
        * @param el {HTMLElement or element id or construct info}
        */
        function radiobox(param) {
            return tui.ctrl.control(param, Radiobox);
        }
        _ctrl.radiobox = radiobox;
        tui.ctrl.registerInitCallback(Radiobox.CLASS, radiobox);
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;
})(tui || (tui = {}));
var tui;
(function (tui) {
    /// <reference path="tui.ctrl.control.ts" />
    /// <reference path="tui.time.ts" />
    (function (_ctrl) {
        var Calendar = (function (_super) {
            __extends(Calendar, _super);
            function Calendar(el) {
                var _this = this;
                _super.call(this, "div", Calendar.CLASS, el);
                this._time = tui.today();
                var self = this;
                this.attr("tabIndex", "0");
                this.selectable(false);
                this[0].innerHTML = "";
                this._tb = this[0].appendChild(document.createElement("table"));
                this._tb.cellPadding = "0";
                this._tb.cellSpacing = "0";
                this._tb.border = "0";
                var yearLine = this._tb.insertRow(-1);
                this._prevMonth = yearLine.insertCell(-1);
                this._prevMonth.className = "tui-prev-month-btn";
                this._prevYear = yearLine.insertCell(-1);
                this._prevYear.className = "tui-prev-year-btn";
                this._yearCell = yearLine.insertCell(-1);
                this._yearCell.colSpan = 3;
                this._nextYear = yearLine.insertCell(-1);
                this._nextYear.className = "tui-next-year-btn";
                this._nextMonth = yearLine.insertCell(-1);
                this._nextMonth.className = "tui-next-month-btn";
                for (var i = 0; i < 7; i++) {
                    var line = this._tb.insertRow(-1);
                    for (var j = 0; j < 7; j++) {
                        var cell = line.insertCell(-1);
                        if (i === 0) {
                            cell.className = "tui-week";
                            this.setText(i + 1, j, tui.str(Calendar._week[j]));
                        }
                    }
                }
                $(this[0]).on("mousedown", function (e) {
                    if (tui.ffVer > 0)
                        _this.focus();
                    if (e.target.nodeName.toLowerCase() !== "td")
                        return;
                    var cell = e.target;
                    if ($(cell).hasClass("tui-prev-month-btn")) {
                        _this.prevMonth();
                    } else if ($(cell).hasClass("tui-prev-year-btn")) {
                        _this.prevYear();
                    } else if ($(cell).hasClass("tui-next-year-btn")) {
                        _this.nextYear();
                    } else if ($(cell).hasClass("tui-next-month-btn")) {
                        _this.nextMonth();
                    } else if (typeof cell["offsetMonth"] === "number") {
                        var d = parseInt(cell.innerHTML, 10);
                        var offset = cell["offsetMonth"];
                        if (offset < 0) {
                            var y = _this.year();
                            var m = _this.month();
                            if (m === 1) {
                                y--;
                                m = 12;
                            } else {
                                m--;
                            }
                            _this.onPicked(y, m, d);
                        } else if (offset > 0) {
                            var y = _this.year();
                            var m = _this.month();
                            if (m === 12) {
                                y++;
                                m = 1;
                            } else {
                                m++;
                            }
                            _this.onPicked(y, m, d);
                        } else if (offset === 0) {
                            _this.onPicked(_this.year(), _this.month(), d);
                        }
                    }
                });
                $(this[0]).on("click", function (e) {
                    if (e.target.nodeName.toLowerCase() !== "td")
                        return;
                    var cell = e.target;
                    if (typeof cell["offsetMonth"] === "number")
                        self.fire("picked", { "ctrl": _this[0], "event": e, "time": _this._time });
                });
                $(this[0]).on("dblclick", function (e) {
                    if (e.target.nodeName.toLowerCase() !== "td")
                        return;
                    var cell = e.target;
                    if (typeof cell["offsetMonth"] === "number")
                        self.fire("dblpicked", { "ctrl": _this[0], "event": e, "time": _this._time });
                });
                $(this[0]).on("keydown", function (e) {
                    var k = e.keyCode;
                    if ([13, 33, 34, 37, 38, 39, 40].indexOf(k) >= 0) {
                        if (k === 37) {
                            var tm = tui.dateAdd(_this._time, -1);
                            self.time(tm);
                        } else if (k === 38) {
                            var tm = tui.dateAdd(_this._time, -7);
                            self.time(tm);
                        } else if (k === 39) {
                            var tm = tui.dateAdd(_this._time, 1);
                            self.time(tm);
                        } else if (k === 40) {
                            var tm = tui.dateAdd(_this._time, 7);
                            self.time(tm);
                        } else if (k === 33) {
                            _this.prevMonth();
                        } else if (k === 34) {
                            _this.nextMonth();
                        } else if (k === 13) {
                            self.fire("picked", { "ctrl": _this[0], "event": e, "time": _this._time });
                        }
                        return e.preventDefault();
                    }
                });
                this.update();
            }
            Calendar.prototype.setText = function (line, column, content) {
                var cell = (this._tb.rows[line].cells[column]);
                if (tui.ieVer > 0 && tui.ieVer < 9) {
                    cell.innerText = content;
                } else
                    cell.innerHTML = content;
            };

            Calendar.prototype.year = function () {
                return this._time.getFullYear();
            };
            Calendar.prototype.day = function () {
                return this._time.getDate();
            };
            Calendar.prototype.month = function () {
                return this._time.getMonth() + 1;
            };

            Calendar.prototype.time = function (t) {
                if (t instanceof Date && t) {
                    var changed = false;
                    if (Math.floor(this._time.getTime() / 1000) !== Math.floor(t.getTime() / 1000))
                        changed = true;
                    this._time = t;
                    this.update();
                    changed && this.fire("change", { "ctrl": this[0], "time": this._time });
                    return this;
                } else
                    return this._time;
            };

            Calendar.prototype.value = function (t) {
                if (t === null) {
                    this.time(tui.today());
                    return this;
                }
                return this.time(t);
            };
            Calendar.prototype.prevMonth = function () {
                var y = this.year(), m = this.month(), d = this.day();
                if (m === 1) {
                    y--;
                    m = 12;
                } else {
                    m--;
                }
                var newDate = new Date(y, m - 1, 1);
                if (d > tui.totalDaysOfMonth(newDate))
                    d = tui.totalDaysOfMonth(newDate);
                this.onPicked(y, m, d);
            };
            Calendar.prototype.nextMonth = function () {
                var y = this.year(), m = this.month(), d = this.day();
                if (m === 12) {
                    y++;
                    m = 1;
                } else {
                    m++;
                }
                var newDate = new Date(y, m - 1, 1);
                if (d > tui.totalDaysOfMonth(newDate))
                    d = tui.totalDaysOfMonth(newDate);
                this.onPicked(y, m, d);
            };
            Calendar.prototype.prevYear = function () {
                var y = this.year(), m = this.month(), d = this.day();
                y--;
                var newDate = new Date(y, m - 1, 1);
                if (d > tui.totalDaysOfMonth(newDate))
                    d = tui.totalDaysOfMonth(newDate);
                this.onPicked(y, m, d);
            };
            Calendar.prototype.nextYear = function () {
                var y = this.year(), m = this.month(), d = this.day();
                y++;
                var newDate = new Date(y, m - 1, 1);
                if (d > tui.totalDaysOfMonth(newDate))
                    d = tui.totalDaysOfMonth(newDate);
                this.onPicked(y, m, d);
            };
            Calendar.prototype.onPicked = function (y, m, d) {
                var newDate = new Date(y, m - 1, d);
                this.time(newDate);
            };
            Calendar.prototype.firstDay = function (date) {
                var y = date.getFullYear();
                var m = date.getMonth();
                return new Date(y, m, 1);
            };
            Calendar.prototype.update = function () {
                var today = tui.today();
                var firstWeek = this.firstDay(this._time).getDay();
                var daysOfMonth = tui.totalDaysOfMonth(this._time);
                var day = 0;
                this._yearCell.innerHTML = this.year() + " - " + this.month();
                for (var i = 0; i < 6; i++) {
                    for (var j = 0; j < 7; j++) {
                        var cell = this._tb.rows[i + 2].cells[j];
                        cell.className = "";
                        if (day === 0) {
                            if (j === firstWeek) {
                                day = 1;
                                cell.innerHTML = day + "";
                                cell.offsetMonth = 0;
                            } else {
                                var preMonthDay = new Date(this.firstDay(this._time).valueOf() - ((firstWeek - j) * 1000 * 24 * 60 * 60));
                                cell.innerHTML = preMonthDay.getDate() + "";
                                cell.offsetMonth = -1;
                                $(cell).addClass("tui-prev-month");
                            }
                        } else {
                            day++;
                            if (day <= daysOfMonth) {
                                cell.innerHTML = day + "";
                                cell.offsetMonth = 0;
                            } else {
                                cell.innerHTML = (day - daysOfMonth) + "";
                                cell.offsetMonth = 1;
                                $(cell).addClass("tui-next-month");
                            }
                        }
                        if (day === this.day())
                            $(cell).addClass("tui-actived");
                        if (j === 0 || j === 6)
                            $(cell).addClass("tui-weekend");
                        if (this.year() === today.getFullYear() && this.month() === (today.getMonth() + 1) && day === today.getDate()) {
                            $(cell).addClass("tui-today");
                        }
                    }
                }
            };
            Calendar.CLASS = "tui-calendar";
            Calendar._week = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
            return Calendar;
        })(_ctrl.Control);
        _ctrl.Calendar = Calendar;

        /**
        * Construct a calendar.
        * @param el {HTMLElement or element id or construct info}
        */
        function calendar(param) {
            return tui.ctrl.control(param, Calendar);
        }
        _ctrl.calendar = calendar;

        tui.ctrl.registerInitCallback(Calendar.CLASS, calendar);
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;
})(tui || (tui = {}));
var tui;
(function (tui) {
    /// <reference path="tui.ctrl.control.ts" />
    (function (ctrl) {
        var _currentPopup = null;

        function closeAllPopup() {
            var pop = _currentPopup;
            while (pop) {
                if (pop.parent())
                    pop = pop.parent();
                else {
                    pop.close();
                    pop = _currentPopup;
                }
            }
        }

        var Popup = (function (_super) {
            __extends(Popup, _super);
            function Popup() {
                _super.call(this, "div", Popup.CLASS, null);
                this._position = null;
                this._bindElem = null;
                this._body = document.body || document.getElementsByTagName("BODY")[0];
                this._parent = null;
                this._parentPopup = null;
                this._childPopup = null;
            }
            Popup.prototype.getParentPopup = function (elem) {
                var pop = _currentPopup;
                while (pop) {
                    if (pop.isPosterity(elem))
                        return pop;
                    else
                        pop = pop.parent();
                }
                return pop;
            };

            Popup.prototype.show = function (content, param, bindType) {
                if (typeof param === "string")
                    param = document.getElementById(param);
                var elem = null;
                if (param && param.nodeName && typeof bindType === "string") {
                    elem = this.elem("div", Popup.CLASS);
                    this._bindElem = param;
                    this._bindType = bindType;
                } else if (param && typeof param.x === "number" && typeof param.y === "number") {
                    elem = this.elem("div", Popup.CLASS);
                    this._position = param;
                }
                if (elem) {
                    if (this._bindElem) {
                        this._parentPopup = this.getParentPopup(this._bindElem);
                        if (this._parentPopup) {
                            this._parentPopup.closeChild();
                            this._parentPopup.child(this);
                            this.parent(this._parentPopup);
                            this._parent = this._parentPopup[0];
                        } else {
                            closeAllPopup();
                            this._parent = this._body;
                        }
                    } else {
                        closeAllPopup();
                        this._parent = this._body;
                    }
                    this._parent.appendChild(elem);
                    elem.focus();
                    _currentPopup = this;
                    elem.setAttribute("tabIndex", "-1");
                    if (typeof content === "string") {
                        elem.innerHTML = content;
                    } else if (content && content.nodeName) {
                        elem.appendChild(content);
                    }
                    tui.ctrl.initCtrls(elem);
                    this.refresh();
                }
            };

            Popup.prototype.close = function () {
                this._parent.removeChild(this[0]);
                _currentPopup = this.parent();
                this.parent(null);
                if (_currentPopup)
                    _currentPopup.child(null);
            };

            Popup.prototype.closeChild = function () {
                if (this._childPopup) {
                    this._childPopup.close();
                    this._childPopup = null;
                }
            };

            Popup.prototype.parent = function (pop) {
                if (typeof pop !== tui.undef) {
                    this._parentPopup = pop;
                }
                return this._parentPopup;
            };

            Popup.prototype.child = function (pop) {
                if (typeof pop !== tui.undef) {
                    this._childPopup = pop;
                }
                return this._childPopup;
            };

            Popup.prototype.refresh = function () {
                if (!this[0])
                    return;
                var elem = this[0];
                var cw = document.documentElement.clientWidth;
                var ch = document.documentElement.clientHeight;
                var sw = elem.offsetWidth;
                var sh = elem.offsetHeight;
                var box = { x: 0, y: 0, w: 0, h: 0 };
                var pos = { x: 0, y: 0 };
                if (this._position) {
                    box = this._position;
                    box.w = 0;
                    box.h = 0;
                } else if (this._bindElem) {
                    box = tui.fixedPosition(this._bindElem);
                    box.w = this._bindElem.offsetWidth;
                    box.h = this._bindElem.offsetHeight;
                }

                // lower case letter means 'next to', upper case letter means 'align to'
                var compute = {
                    "l": function () {
                        pos.x = box.x - sw;
                        if (pos.x < 2)
                            pos.x = box.x + box.w;
                    }, "r": function () {
                        pos.x = box.x + box.w;
                        if (pos.x + sw > cw - 2)
                            pos.x = box.x - sw;
                    }, "t": function () {
                        pos.y = box.y - sh;
                        if (pos.y < 2)
                            pos.y = box.y + box.h;
                    }, "b": function () {
                        pos.y = box.y + box.h;
                        if (pos.y + sh > ch - 2)
                            pos.y = box.y - sh;
                    }, "L": function () {
                        pos.x = box.x;
                        if (pos.x + sw > cw - 2)
                            pos.x = box.x + box.w - sw;
                    }, "R": function () {
                        pos.x = box.x + box.w - sw;
                        if (pos.x < 2)
                            pos.x = box.x;
                    }, "T": function () {
                        pos.y = box.y;
                        if (pos.y + sh > ch - 2)
                            pos.y = box.y + box.h - sh;
                    }, "B": function () {
                        pos.y = box.y + box.h - sh;
                        if (pos.y < 2)
                            pos.y = box.y;
                    }
                };
                compute[this._bindType.substring(0, 1)](); // parse x
                compute[this._bindType.substring(1, 2)](); // parse y

                if (pos.x > cw - 2)
                    pos.x = cw - 2;
                if (pos.x < 2)
                    pos.x = 2;
                if (pos.y > ch - 2)
                    pos.y = ch - 2;
                if (pos.y < 2)
                    pos.y = 2;

                elem.style.left = pos.x + 2 + "px";
                elem.style.top = pos.y + 2 + "px";
            };
            Popup.CLASS = "tui-popup";
            return Popup;
        })(ctrl.Control);
        ctrl.Popup = Popup;

        function checkPopup() {
            setTimeout(function () {
                var obj = document.activeElement;
                while (_currentPopup) {
                    if (_currentPopup.isPosterity(obj))
                        return;
                    else
                        _currentPopup.close();
                }
            }, 30);
        }
        ctrl.checkPopup = checkPopup;

        $(document).on("focus mousedown keydown", checkPopup);
        tui.on("#tui.check.popup", checkPopup);

        $(window).scroll(function () {
            closeAllPopup();
        });

        /**
        * Construct a button.
        * @param el {HTMLElement or element id or construct info}
        */
        function popup() {
            return tui.ctrl.control(null, Popup);
        }
        ctrl.popup = popup;
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;
})(tui || (tui = {}));
/// <reference path="tui.ctrl.control.ts" />
var tui;
(function (tui) {
    (function (ctrl) {
        var _dialogStack = [];
        var _mask = document.createElement("div");
        _mask.className = "tui-dialog-mask";
        _mask.setAttribute("unselectable", "on");
        var mousewheelevt = (/Firefox/i.test(navigator.userAgent)) ? "DOMMouseScroll" : "mousewheel";
        $(_mask).on(mousewheelevt, function (ev) {
            ev.stopPropagation();
            ev.preventDefault();
        });

        function reorder() {
            if (_mask.parentNode !== null) {
                _mask.parentNode.removeChild(_mask);
            }
            if (_dialogStack.length > 0) {
                document.body.insertBefore(_mask, _dialogStack[_dialogStack.length - 1].elem());
            } else {
            }
        }

        function push(dlg) {
            _dialogStack.push(dlg);
            document.body.appendChild(dlg.elem());
            reorder();
        }

        function remove(dlg) {
            var index = _dialogStack.indexOf(dlg);
            if (index >= 0) {
                _dialogStack.splice(index, 1);
            }
            document.body.removeChild(dlg.elem());
            reorder();
        }

        function getParent(dlg) {
            var index = _dialogStack.indexOf(dlg);
            if (index > 0) {
                _dialogStack[index - 1];
            } else
                return null;
        }

        var Dialog = (function (_super) {
            __extends(Dialog, _super);
            function Dialog() {
                _super.call(this, "div", Dialog.CLASS, null);
                this._resourceElement = null;
                this._isMoved = false;
                this._isInitialize = true;
                this._titleText = null;
                this._noRefresh = false;
                this._useEsc = true;
            }
            Dialog.prototype.showContent = function (content, title, buttons) {
                if (this[0])
                    return this;
                this._resourceElement = null;
                return this.showElement(tui.toElement(content), title, buttons);
            };

            Dialog.prototype.showResource = function (elemId, title, buttons) {
                if (this[0])
                    return this;
                var elem = document.getElementById(elemId);
                if (!elem) {
                    throw new Error("Resource id not found: " + elemId);
                }
                this._resourceElement = elem;
                return this.showElement(elem, title, buttons);
            };

            Dialog.prototype.showElement = function (elem, title, buttons) {
                var _this = this;
                if (this[0])
                    return this;

                // Temporary inhibit refresh to prevent unexpected calculation
                this._noRefresh = true;
                this.elem("div", Dialog.CLASS);
                this.attr("tabIndex", "-1");
                this._titleDiv = document.createElement("div");
                this._titleDiv.className = "tui-dlg-title-bar";
                this._titleDiv.setAttribute("unselectable", "on");
                this._titleDiv.onselectstart = function () {
                    return false;
                };
                this[0].appendChild(this._titleDiv);
                this._closeIcon = document.createElement("span");
                this._closeIcon.className = "tui-dlg-close";
                this._titleDiv.appendChild(this._closeIcon);
                this._contentDiv = document.createElement("div");
                this[0].appendChild(this._contentDiv);
                this._buttonDiv = document.createElement("div");
                this._buttonDiv.className = "tui-dlg-btn-bar";
                this[0].appendChild(this._buttonDiv);
                var tt = "";
                if (typeof title === "string") {
                    tt = title;
                } else {
                    if (elem.title) {
                        tt = elem.title;
                    }
                }
                this.title(tt);
                this._contentDiv.appendChild(elem);
                $(elem).removeClass("tui-hidden");
                var self = this;
                if (buttons && typeof buttons.length === "number") {
                    for (var i = 0; i < buttons.length; i++) {
                        this.insertButton(buttons[i]);
                    }
                } else {
                    this.insertButton({
                        name: tui.str("Ok"),
                        func: function (data) {
                            self.close();
                        }
                    });
                }

                // Add to document
                push(this);

                // Convert all child elements into tui controls
                tui.ctrl.initCtrls(elem);
                this._isInitialize = true;
                this._isMoved = false;

                $(this._closeIcon).on("click", function () {
                    _this.close();
                });

                $(this._titleDiv).on("mousedown", function (e) {
                    if (e.target === _this._closeIcon)
                        return;
                    var dialogX = _this[0].offsetLeft;
                    var dialogY = _this[0].offsetTop;
                    var beginX = e.clientX;
                    var beginY = e.clientY;
                    var winSize = { width: _mask.offsetWidth, height: _mask.offsetHeight };
                    tui.mask();
                    function onMoveEnd(e) {
                        tui.unmask();
                        $(document).off("mousemove", onMove);
                        $(document).off("mouseup", onMoveEnd);
                    }
                    function onMove(e) {
                        var l = dialogX + e.clientX - beginX;
                        var t = dialogY + e.clientY - beginY;
                        if (l > winSize.width - self[0].offsetWidth)
                            l = winSize.width - self[0].offsetWidth;
                        if (l < 0)
                            l = 0;
                        if (t > winSize.height - self[0].offsetHeight)
                            t = winSize.height - self[0].offsetHeight;
                        if (t < 0)
                            t = 0;
                        self[0].style.left = l + "px";
                        self[0].style.top = t + "px";
                        self._isMoved = true;
                    }
                    $(document).on("mousemove", onMove);
                    $(document).on("mouseup", onMoveEnd);
                });
                $(this[0]).on(mousewheelevt, function (ev) {
                    ev.stopPropagation();
                    ev.preventDefault();
                });

                // After initialization finished preform refresh now.
                this._noRefresh = false;
                this[0].style.left = "0px";
                this[0].style.top = "0px";
                this.limitSize();
                this.refresh();
                this[0].focus();
                this.fire("open");
                return this;
            };

            Dialog.prototype.limitSize = function () {
                var _this = this;
                setTimeout(function () {
                    _this._contentDiv.style.maxHeight = "";
                    _this[0].style.maxWidth = _mask.offsetWidth + "px";
                    _this[0].style.maxHeight = _mask.offsetHeight + "px";
                    _this._contentDiv.style.maxHeight = _this[0].clientHeight - _this._titleDiv.offsetHeight - _this._buttonDiv.offsetHeight - $(_this._contentDiv).outerHeight() + $(_this._contentDiv).height() + "px";
                    _this.refresh();
                }, 0);
            };

            Dialog.prototype.insertButton = function (btn, index) {
                if (!this[0])
                    return null;
                var button = tui.ctrl.button();
                button.text(btn.name);
                btn.id && button.id(btn.id);
                btn.cls && button.addClass(btn.cls);
                btn.func && button.on("click", btn.func);
                if (typeof index === "number" && !isNaN(index)) {
                    var refButton = this._buttonDiv.childNodes[index];
                    if (refButton)
                        this._buttonDiv.insertBefore(button.elem(), refButton);
                    else
                        this._buttonDiv.appendChild(button.elem());
                } else {
                    this._buttonDiv.appendChild(button.elem());
                }
                this.refresh();
                return button;
            };

            Dialog.prototype.removeButton = function (btn) {
                if (!this[0])
                    return;
                var refButton;
                if (typeof btn === "number") {
                    refButton = this._buttonDiv.childNodes[btn];
                } else if (btn instanceof ctrl.Button) {
                    refButton = btn.elem();
                }
                this._buttonDiv.removeChild(refButton);
            };

            Dialog.prototype.button = function (index) {
                if (!this[0])
                    return null;
                var refButton = this._buttonDiv.childNodes[index];
                if (refButton) {
                    return tui.ctrl.button(refButton);
                } else
                    return null;
            };

            Dialog.prototype.removeAllButtons = function () {
                if (!this[0])
                    return;
                this._buttonDiv.innerHTML = "";
            };

            Dialog.prototype.useesc = function (val) {
                if (typeof val === "boolean") {
                    this._useEsc = val;
                    this.title(this.title());
                } else {
                    return this._useEsc;
                }
            };

            Dialog.prototype.title = function (t) {
                if (typeof t === "string") {
                    if (!this[0])
                        return this;
                    if (this._closeIcon.parentNode)
                        this._closeIcon.parentNode.removeChild(this._closeIcon);
                    this._titleDiv.innerHTML = t;
                    if (this._useEsc)
                        this._titleDiv.appendChild(this._closeIcon);
                    this._titleText = t;
                    this.refresh();
                    return this;
                } else {
                    if (!this[0])
                        return null;
                    return this._titleText;
                }
            };

            Dialog.prototype.close = function () {
                if (!this[0])
                    return;
                remove(this);
                this.elem(null);
                this._titleDiv = null;
                this._contentDiv = null;
                this._buttonDiv = null;
                this._closeIcon = null;
                this._titleText = null;
                if (this._resourceElement) {
                    $(this._resourceElement).addClass("tui-hidden");
                    document.body.appendChild(this._resourceElement);
                    this._resourceElement = null;
                }
                this.fire("close");
            };

            Dialog.prototype.refresh = function () {
                if (!this[0])
                    return;
                if (this._noRefresh)
                    return;

                // Change position
                var winSize = { width: _mask.offsetWidth, height: _mask.offsetHeight };

                var box = {
                    left: this[0].offsetLeft,
                    top: this[0].offsetTop,
                    width: this[0].offsetWidth,
                    height: this[0].offsetHeight
                };
                if (this._isInitialize) {
                    var parent = getParent(this);
                    var centX, centY;
                    if (parent) {
                        var e = parent.elem();
                        centX = e.offsetLeft + e.offsetWidth / 2;
                        centY = e.offsetTop + e.offsetHeight / 2;
                        this._isMoved = true;
                    } else {
                        centX = winSize.width / 2;
                        centY = winSize.height / 2;
                        this._isMoved = false;
                    }
                    box.left = centX - box.width / 2;
                    box.top = centY - box.height / 2;
                    this._isInitialize = false;
                } else {
                    if (!this._isMoved) {
                        box.left = (winSize.width - box.width) / 2;
                        box.top = (winSize.height - box.height) / 2;
                    }
                }
                if (box.left + box.width > winSize.width)
                    box.left = winSize.width - box.width;
                if (box.top + box.height > winSize.height)
                    box.top = winSize.height - box.height;
                if (box.left < 0)
                    box.left = 0;
                if (box.top < 0)
                    box.top = 0;
                this[0].style.left = box.left + "px";
                this[0].style.top = box.top + "px";
            };
            Dialog.CLASS = "tui-dialog";
            return Dialog;
        })(ctrl.Control);
        ctrl.Dialog = Dialog;

        /**
        * Construct a button.
        * @param el {HTMLElement or element id or construct info}
        */
        function dialog() {
            return tui.ctrl.control(null, Dialog);
        }
        ctrl.dialog = dialog;

        $(document).on("keydown", function (e) {
            var k = e.keyCode;
            if (_dialogStack.length <= 0)
                return;
            var dlg = _dialogStack[_dialogStack.length - 1];
            if (k === 27) {
                dlg.useesc() && dlg.close();
            } else if (k === 9) {
                setTimeout(function () {
                    if (!dlg.isPosterity(document.activeElement)) {
                        dlg.focus();
                    }
                }, 0);
            }
        });

        $(window).resize(function () {
            for (var i = 0; i < _dialogStack.length; i++) {
                _dialogStack[i].limitSize();
                _dialogStack[i].refresh();
            }
        });
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;

    function msgbox(message, title) {
        var dlg = tui.ctrl.dialog();
        var wrap = document.createElement("div");
        wrap.className = "tui-dlg-msg";
        wrap.innerHTML = message;
        dlg.showElement(wrap, title);
        return dlg;
    }
    tui.msgbox = msgbox;

    function infobox(message, title) {
        var dlg = tui.ctrl.dialog();
        var wrap = document.createElement("div");
        wrap.className = "tui-dlg-warp tui-dlg-info";
        wrap.innerHTML = message;
        dlg.showElement(wrap, title);
        return dlg;
    }
    tui.infobox = infobox;

    function okbox(message, title) {
        var dlg = tui.ctrl.dialog();
        var wrap = document.createElement("div");
        wrap.className = "tui-dlg-warp tui-dlg-ok";
        wrap.innerHTML = message;
        dlg.showElement(wrap, title);
        return dlg;
    }
    tui.okbox = okbox;

    function errbox(message, title) {
        var dlg = tui.ctrl.dialog();
        var wrap = document.createElement("div");
        wrap.className = "tui-dlg-warp tui-dlg-err";
        wrap.innerHTML = message;
        dlg.showElement(wrap, title);
        return dlg;
    }
    tui.errbox = errbox;

    function warnbox(message, title) {
        var dlg = tui.ctrl.dialog();
        var wrap = document.createElement("div");
        wrap.className = "tui-dlg-warp tui-dlg-warn";
        wrap.innerHTML = message;
        dlg.showElement(wrap, title);
        return dlg;
    }
    tui.warnbox = warnbox;

    function askbox(message, title, callback) {
        var dlg = tui.ctrl.dialog();
        var wrap = document.createElement("div");
        wrap.className = "tui-dlg-warp tui-dlg-ask";
        wrap.innerHTML = message;
        var result = false;
        dlg.showElement(wrap, title, [
            {
                name: tui.str("Ok"), func: function () {
                    result = true;
                    dlg.close();
                }
            }, {
                name: tui.str("Cancel"), func: function () {
                    dlg.close();
                }
            }
        ]);
        dlg.on("close", function () {
            if (typeof callback === "function")
                callback(result);
        });
        return dlg;
    }
    tui.askbox = askbox;

    function waitbox(message) {
        var dlg = tui.ctrl.dialog();
        var wrap = document.createElement("div");
        wrap.className = "tui-dlg-warp tui-dlg-wait";
        wrap.innerHTML = message;
        dlg.showElement(wrap, null, []);
        dlg.useesc(false);
        return dlg;
    }
    tui.waitbox = waitbox;
})(tui || (tui = {}));
var tui;
(function (tui) {
    /// <reference path="tui.ctrl.control.ts" />
    (function (ctrl) {
        var Scrollbar = (function (_super) {
            __extends(Scrollbar, _super);
            function Scrollbar(el) {
                var _this = this;
                _super.call(this, "span", Scrollbar.CLASS, el);
                this._btnThumb = null;
                this._btnHead = null;
                this._btnFoot = null;
                var self = this;

                this.attr("unselectable", "on");
                this[0].innerHTML = "";
                this._btnHead = document.createElement("span");
                this._btnHead.className = "tui-scroll-head";
                this[0].appendChild(this._btnHead);
                this._btnThumb = document.createElement("span");
                this._btnThumb.className = "tui-scroll-thumb";
                $(this._btnThumb).attr("unselectable", "on");
                this[0].appendChild(this._btnThumb);
                this._btnFoot = document.createElement("span");
                this._btnFoot.className = "tui-scroll-foot";
                this[0].appendChild(this._btnFoot);

                var scrollTimer = null;
                var scrollInterval = null;
                var moveParam = null;

                function stopMove() {
                    if (scrollTimer) {
                        clearTimeout(scrollTimer);
                        scrollTimer = null;
                    }
                    if (scrollInterval) {
                        clearInterval(scrollInterval);
                        scrollInterval = null;
                    }
                }

                function moveThumb() {
                    var val = self.value();
                    var total = self.total();
                    var achieve = false;
                    moveParam.pos = Math.round(moveParam.pos);
                    moveParam.step = Math.round(moveParam.step);
                    if (val === moveParam.pos)
                        return;
                    if (!moveParam.isIncrease) {
                        val -= moveParam.step;
                        if (val - (moveParam.isPage ? moveParam.step / 2 : 0) <= moveParam.pos || val <= 0) {
                            achieve = true;
                            if (val < 0)
                                val = 0;
                            stopMove();
                        }
                        self.value(val);
                    } else {
                        val += moveParam.step;
                        if (val + (moveParam.isPage ? moveParam.step / 2 : 0) >= moveParam.pos || val >= total) {
                            achieve = true;
                            if (val > total)
                                val = total;
                            stopMove();
                        }
                        self.value(val);
                    }
                    self.fire("scroll", { value: self.value(), type: "mousedown" });
                    return achieve;
                }

                function releaseButton(e) {
                    stopMove();
                    $(self._btnHead).removeClass("tui-actived");
                    $(self._btnFoot).removeClass("tui-actived");
                    $(tui.unmask()).off("mouseup", releaseButton);
                    $(document).off("mouseup", releaseButton);
                }
                ;

                $(this[0]).mousedown(function (e) {
                    tui.fire("#tui.check.popup");

                    // Should check which target object was triggered.
                    if (!tui.isLButton(e.button)) {
                        return;
                    }
                    var obj = e.target;
                    if (obj !== self[0]) {
                        e.stopPropagation();
                        e.preventDefault();
                        return;
                    }
                    if (_this.total() <= 0)
                        return;
                    var dir = self.direction();
                    var pos, thumbLen;

                    if (dir === "vertical") {
                        pos = (typeof e.offsetY === "number" ? e.offsetY : e["originalEvent"].layerY);
                        thumbLen = _this._btnThumb.offsetHeight;
                    } else {
                        pos = (typeof e.offsetX === "number" ? e.offsetX : e["originalEvent"].layerX);
                        thumbLen = _this._btnThumb.offsetWidth;
                    }
                    var v = _this.posToValue(pos - thumbLen / 2);
                    moveParam = { pos: v, step: self.page(), isIncrease: v > self.value(), isPage: true };
                    if (!moveThumb()) {
                        scrollTimer = setTimeout(function () {
                            scrollTimer = null;
                            scrollInterval = setInterval(moveThumb, 20);
                        }, 300);
                        $(tui.mask()).on("mouseup", releaseButton);
                        $(document).on("mouseup", releaseButton);
                    }
                    e.stopPropagation();
                    e.preventDefault();
                    return false;
                });

                $(this._btnHead).mousedown(function (e) {
                    if (!tui.isLButton(e.button))
                        return;
                    if (self.total() <= 0)
                        return;
                    $(self._btnHead).addClass("tui-actived");
                    moveParam = { pos: 0, step: self.step(), isIncrease: false, isPage: false };
                    if (!moveThumb()) {
                        scrollTimer = setTimeout(function () {
                            scrollTimer = null;
                            scrollInterval = setInterval(moveThumb, 20);
                        }, 300);
                        $(tui.mask()).on("mouseup", releaseButton);
                        $(document).on("mouseup", releaseButton);
                    }
                });

                $(this._btnFoot).mousedown(function (e) {
                    if (!tui.isLButton(e.button))
                        return;
                    if (self.total() <= 0)
                        return;
                    $(self._btnFoot).addClass("tui-actived");
                    moveParam = { pos: self.total(), step: self.step(), isIncrease: true, isPage: false };
                    if (!moveThumb()) {
                        scrollTimer = setTimeout(function () {
                            scrollTimer = null;
                            scrollInterval = setInterval(moveThumb, 20);
                        }, 300);
                        $(tui.mask()).on("mouseup", releaseButton);
                        $(document).on("mouseup", releaseButton);
                    }
                });

                var mousewheelevt = (/Firefox/i.test(navigator.userAgent)) ? "DOMMouseScroll" : "mousewheel";
                $(this[0]).on(mousewheelevt, function (e) {
                    var ev = e.originalEvent;
                    var delta = ev.detail ? ev.detail * (-120) : ev.wheelDelta;

                    //delta returns +120 when wheel is scrolled up, -120 when scrolled down
                    var scrollSize = (Math.round(self.page() / 2) > self.step() ? Math.round(self.page() / 2) : self.step());
                    var oldValue = self.value();
                    if (delta <= -120) {
                        self.value(self.value() + scrollSize);
                    } else {
                        self.value(self.value() - scrollSize);
                    }
                    if (oldValue !== self.value())
                        self.fire("scroll", { value: self.value(), type: "mousewheel" });
                    e.stopPropagation();
                    e.preventDefault();
                });

                var beginX = 0, beginY = 0, beginLeft = 0, beginTop = 0;
                function dragThumb(e) {
                    var diff = 0;
                    var oldValue = self.value();
                    var pos;
                    if (self.direction() === "vertical") {
                        diff = e.clientY - beginY;
                        pos = beginTop + diff;
                    } else {
                        diff = e.clientX - beginX;
                        pos = beginLeft + diff;
                    }
                    self.value(self.posToValue(pos));
                    if (oldValue !== self.value())
                        self.fire("scroll", { value: self.value(), type: "drag" });
                }

                function dragEnd(e) {
                    $(tui.unmask()).off("mousemove", dragThumb);
                    $(document).off("mouseup", dragEnd);
                    $(self._btnThumb).removeClass("tui-actived");
                    self.fire("dragend", { value: self.value() });
                }

                $(this._btnThumb).mousedown(function (e) {
                    if (!tui.isLButton(e.button))
                        return;
                    beginX = e.clientX;
                    beginY = e.clientY;
                    beginLeft = self._btnThumb.offsetLeft;
                    beginTop = self._btnThumb.offsetTop;
                    $(self._btnThumb).addClass("tui-actived");
                    $(tui.mask()).on("mousemove", dragThumb);
                    $(document).on("mouseup", dragEnd);
                    self.fire("dragbegin", { value: self.value() });
                });

                this.refresh();
            }
            Scrollbar.prototype.total = function (val) {
                if (typeof val === "number") {
                    if (val < 0)
                        val = 0;
                    val = Math.round(val);
                    this.attr("data-total", val);
                    if (this.value() > val)
                        this.value(val);
                    else
                        this.refresh();
                    return this;
                } else {
                    var val = parseInt(this.attr("data-total"), 10);
                    if (val === null || isNaN(val))
                        return 0;
                    else
                        return val;
                }
            };

            Scrollbar.prototype.value = function (val) {
                if (typeof val === "number") {
                    val = Math.round(val);
                    if (val < 0)
                        val = 0;
                    if (val > this.total())
                        val = this.total();
                    this.attr("data-value", val);
                    this.refresh();
                    return this;
                } else {
                    var val = parseInt(this.attr("data-value"), 10);
                    if (val === null || isNaN(val))
                        return 0;
                    else
                        return val;
                }
            };

            Scrollbar.prototype.step = function (val) {
                if (typeof val === "number") {
                    val = Math.round(val);
                    if (val < 1)
                        val = 1;
                    if (val > this.total())
                        val = this.total();
                    this.attr("data-step", val);
                    if (val > this.page())
                        this.page(val);
                    else
                        this.refresh();
                    return this;
                } else {
                    var val = parseInt(this.attr("data-step"), 10);
                    if (val === null || isNaN(val))
                        return this.total() > 0 ? 1 : 0;
                    else
                        return val;
                }
            };

            Scrollbar.prototype.page = function (val) {
                if (typeof val === "number") {
                    val = Math.round(val);
                    if (val < 1)
                        val = 1;
                    if (val > this.total())
                        val = this.total();
                    this.attr("data-page", val);
                    if (val < this.step())
                        this.step(val);
                    else
                        this.refresh();
                    return this;
                } else {
                    var val = parseInt(this.attr("data-page"), 10);
                    if (val === null || isNaN(val))
                        return this.total() > 0 ? 1 : 0;
                    else
                        return val;
                }
            };

            Scrollbar.prototype.direction = function (val) {
                if (typeof val === "string") {
                    if (["horizontal", "vertical"].indexOf(val) >= 0) {
                        this.attr("data-direction", val);
                        this.refresh();
                    }
                    return this;
                } else {
                    var dir = this.attr("data-direction");
                    if (dir === null)
                        return "vertical";
                    else
                        return dir;
                }
            };

            Scrollbar.prototype.logicLenToRealLen = function (logicLen) {
                var len = 0;
                var total = this.total();
                if (total <= 0)
                    return 0;
                if (this.direction() === "vertical") {
                    len = this[0].clientHeight - this._btnHead.offsetHeight - this._btnFoot.offsetHeight - this._btnThumb.offsetHeight;
                } else {
                    len = this[0].clientWidth - this._btnHead.offsetWidth - this._btnFoot.offsetWidth - this._btnThumb.offsetWidth;
                }
                return logicLen / total * len;
            };

            Scrollbar.prototype.posToValue = function (pos) {
                var total = this.total();
                if (total <= 0) {
                    return 0;
                }
                var len = 0;
                var val = 0;
                if (this.direction() === "vertical") {
                    len = this[0].clientHeight - this._btnHead.offsetHeight - this._btnFoot.offsetHeight - this._btnThumb.offsetHeight;
                    val = (pos - this._btnHead.offsetHeight) / len * total;
                } else {
                    len = this[0].clientWidth - this._btnHead.offsetWidth - this._btnFoot.offsetWidth - this._btnThumb.offsetWidth;
                    val = (pos - this._btnHead.offsetWidth) / len * total;
                }
                val = Math.round(val);
                return val;
            };

            Scrollbar.prototype.valueToPos = function (value) {
                var total = this.total();
                var step = this.step();
                var page = this.page();
                var vertical = (this.direction() === "vertical");
                var minSize = (vertical ? this._btnHead.offsetHeight : this._btnHead.offsetWidth);
                if (total <= 0) {
                    return { pos: 0, thumbLen: 0 };
                }
                var len = (vertical ? this[0].clientHeight - this._btnHead.offsetHeight - this._btnFoot.offsetHeight : this[0].clientWidth - this._btnHead.offsetWidth - this._btnFoot.offsetWidth);
                var thumbLen = Math.round(page / total * len);
                if (thumbLen < minSize)
                    thumbLen = minSize;
                if (thumbLen > len - 10)
                    thumbLen = len - 10;
                var scale = (value / total);
                if (scale < 0)
                    scale = 0;
                if (scale > 1)
                    scale = 1;
                var pos = minSize + Math.round(scale * (len - thumbLen)) - 1;
                return {
                    "pos": pos, "thumbLen": thumbLen
                };
            };

            Scrollbar.prototype.refresh = function () {
                var pos = this.valueToPos(this.value());
                var vertical = (this.direction() === "vertical");
                if (vertical) {
                    this._btnThumb.style.height = (pos.thumbLen > 0 ? pos.thumbLen : 0) + "px";
                    this._btnThumb.style.top = pos.pos + "px";
                    this._btnThumb.style.left = "";
                    this._btnThumb.style.width = "";
                } else {
                    this._btnThumb.style.width = (pos.thumbLen > 0 ? pos.thumbLen : 0) + "px";
                    this._btnThumb.style.left = pos.pos + "px";
                    this._btnThumb.style.top = "";
                    this._btnThumb.style.height = "";
                }
            };
            Scrollbar.CLASS = "tui-scrollbar";
            return Scrollbar;
        })(ctrl.Control);
        ctrl.Scrollbar = Scrollbar;

        /**
        * Construct a scrollbar.
        * @param el {HTMLElement or element id or construct info}
        */
        function scrollbar(param) {
            return tui.ctrl.control(param, Scrollbar);
        }
        ctrl.scrollbar = scrollbar;

        tui.ctrl.registerInitCallback(Scrollbar.CLASS, scrollbar);
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;
})(tui || (tui = {}));
var tui;
(function (tui) {
    /// <reference path="tui.ctrl.control.ts" />
    (function (ctrl) {
        var Table = (function (_super) {
            __extends(Table, _super);
            function Table(el) {
                _super.call(this, "table", ctrl.Grid.CLASS, el);
                this._splitters = [];
                this._columns = [];
                this._data = null;
                var self = this;

                this.addClass(Table.CLASS);
                this._columns = [];
                var noHead = this.noHead();
                var headLine = this.headLine();
                var headKeys = [];
                if (headLine) {
                    for (var i = 0; i < headLine.cells.length; i++) {
                        var cell = headLine.cells[i];
                        var colKey = $(cell).attr("data-key");
                        if (!noHead) {
                            var col = {
                                name: cell.innerHTML,
                                key: colKey ? colKey : i
                            };
                            headKeys.push(colKey ? colKey : i);
                            this._columns.push(col);
                        } else {
                            headKeys.push(i);
                            this._columns.push({ name: "", key: i });
                        }
                    }
                } else {
                    if (!this.hasAttr("data-columns")) {
                        this._columns = [];
                    }
                }
                var data = {
                    head: headKeys,
                    data: []
                };
                for (var i = noHead ? 0 : 1; i < this[0].rows.length; i++) {
                    var row = this[0].rows[i];
                    var rowData = [];
                    for (var j = 0; j < this._columns.length; j++) {
                        rowData.push(row.cells[j].innerHTML);
                    }
                    data.data.push(rowData);
                }
                this.data(data);
            }
            Table.prototype.headLine = function () {
                var tb = this[0];
                if (!tb)
                    return null;
                return tb.rows[0];
            };

            Table.prototype.createSplitters = function () {
                var self = this;
                this._splitters.length = 0;
                var tb = this[0];
                if (!tb)
                    return;
                var headLine = this.headLine();
                if (!headLine)
                    return;
                if (this.noHead())
                    return;

                for (var i = 0; i < this._splitters.length; i++) {
                    tui.removeNode(this._splitters[i]);
                }
                if (this.resizable()) {
                    for (var i = 0; i < headLine.cells.length; i++) {
                        var cell = headLine.cells[i];
                        var splitter = document.createElement("span");
                        splitter["colIndex"] = i;
                        splitter.className = "tui-table-splitter";
                        if (typeof this._columns[i].width !== "number")
                            this._columns[i].width = $(cell).width();
                        $(splitter).attr("unselectable", "on");
                        headLine.cells[i].appendChild(splitter);
                        this._splitters.push(splitter);
                        $(splitter).mousedown(function (e) {
                            var target = e.target;
                            var l = target.offsetLeft;
                            var srcX = e.clientX;
                            target.style.height = self[0].clientHeight + "px";
                            target.style.bottom = "";
                            $(target).addClass("tui-splitter-move");
                            var mask = tui.mask();
                            mask.style.cursor = "col-resize";

                            function onDragEnd(e) {
                                $(document).off("mousemove", onDrag);
                                $(document).off("mouseup", onDragEnd);
                                $(target).removeClass("tui-splitter-move");
                                tui.unmask();
                                var colIndex = target["colIndex"];
                                var tmpWidth = self._columns[colIndex].width + e.clientX - srcX;
                                if (tmpWidth < 0)
                                    tmpWidth = 0;
                                self._columns[colIndex].width = tmpWidth;
                                self._columns[colIndex].important = true;
                                self.refresh();
                                self.fire("resizecolumn", colIndex);
                            }
                            function onDrag(e) {
                                target.style.left = l + e.clientX - srcX + "px";
                            }

                            $(document).mousemove(onDrag);
                            $(document).mouseup(onDragEnd);
                        });
                    }
                }
            };

            Table.prototype.noHead = function (val) {
                if (typeof val === "boolean") {
                    this.is("data-no-head", val);
                    this.data(this.data());
                    return this;
                } else
                    return this.is("data-no-head");
            };

            Table.prototype.columns = function (val) {
                if (val) {
                    this._columns = val;
                    this.data(this.data());
                    return this;
                } else {
                    if (!this._columns) {
                        var valstr = this.attr("data-columns");
                        this._columns = eval("(" + valstr + ")");
                    }
                    return this._columns;
                }
            };

            Table.prototype.value = function (data) {
                if (data === null) {
                    return this.data([]);
                } else if (data) {
                    return this.data(data);
                } else {
                    var result = [];
                    var dt = this.data();
                    for (var i = 0; i < dt.length(); i++) {
                        result.push(dt.at(i));
                    }
                    return result;
                }
            };

            Table.prototype.data = function (data) {
                if (data) {
                    var self = this;
                    if (data instanceof Array || data.data && data.data instanceof Array) {
                        data = new tui.ArrayProvider(data);
                    }
                    if (typeof data.length !== "function" || typeof data.sort !== "function" || typeof data.at !== "function" || typeof data.columnKeyMap !== "function") {
                        throw new Error("TUI Table: need a data provider.");
                    }

                    var tb = this[0];
                    while (tb.rows.length > 0) {
                        tb.deleteRow(0);
                    }
                    if (!this.noHead()) {
                        var row = tb.insertRow(-1);
                        for (var j = 0; j < this._columns.length; j++) {
                            var cell = row.insertCell(-1);
                            cell.className = "tui-table-head";
                            if (["center", "left", "right"].indexOf(this._columns[j].headAlign) >= 0)
                                cell.style.textAlign = this._columns[j].headAlign;
                            var contentDiv = cell.appendChild(document.createElement("div"));
                            contentDiv.innerHTML = this._columns[j].name;
                        }
                    }
                    for (var i = 0; i < data.length(); i++) {
                        var rowData = data.at(i);
                        var row = tb.insertRow(-1);
                        for (var j = 0; j < this._columns.length; j++) {
                            var cell = row.insertCell(-1);
                            if (["center", "left", "right"].indexOf(this._columns[j].align) >= 0)
                                cell.style.textAlign = this._columns[j].align;
                            var contentDiv = cell.appendChild(document.createElement("div"));
                            var key;
                            if (this._columns[j].key) {
                                key = this._columns[j].key;
                            } else {
                                key = j;
                            }
                            contentDiv.innerHTML = rowData[key];
                        }
                    }
                    this.createSplitters();
                    this.refresh();
                    return this;
                } else {
                    return this._data;
                }
            };

            Table.prototype.resizable = function (val) {
                if (typeof val === "boolean") {
                    this.is("data-resizable", val);
                    this.createSplitters();
                    this.refresh();
                    return this;
                } else
                    return this.is("data-resizable");
            };

            Table.prototype.refresh = function () {
                if (!this.resizable())
                    return;
                var tb = this[0];
                if (!tb)
                    return;
                var headLine = tb.rows[0];
                if (!headLine)
                    return;
                var cellPadding = headLine.cells.length > 0 ? $(headLine.cells[0]).outerWidth() - $(headLine.cells[0]).width() : 0;
                var defaultWidth = Math.floor(tb.offsetWidth / (headLine.cells.length > 0 ? headLine.cells.length : 1) - cellPadding);
                var totalWidth = 0;
                var computeWidth = tb.offsetWidth - cellPadding * (headLine.cells.length > 0 ? headLine.cells.length : 1);
                for (var i = 0; i < this._columns.length; i++) {
                    if (typeof this._columns[i].width !== "number") {
                        this._columns[i].width = defaultWidth;
                        totalWidth += defaultWidth;
                    } else if (!this._columns[i].important) {
                        totalWidth += this._columns[i].width;
                    } else {
                        if (this._columns[i].width > computeWidth)
                            this._columns[i].width = computeWidth;
                        if (this._columns[i].width < 1)
                            this._columns[i].width = 1;
                        computeWidth -= this._columns[i].width;
                    }
                }
                for (var i = 0; i < this._columns.length; i++) {
                    if (!this._columns[i].important) {
                        if (totalWidth === 0)
                            this._columns[i].width = 0;
                        else
                            this._columns[i].width = Math.floor(this._columns[i].width / totalWidth * computeWidth);
                        if (this._columns[i].width < 1)
                            this._columns[i].width = 1;
                    } else {
                        this._columns[i].important = false;
                    }
                    if (tb.rows.length > 0) {
                        var row = tb.rows[0];
                        $(row.cells[i]).css("width", this._columns[i].width + "px");
                    }
                }
                var headLine = this.headLine();
                for (var i = 0; i < this._splitters.length; i++) {
                    var splitter = this._splitters[i];
                    var cell = headLine.cells[i];
                    splitter.style.left = cell.offsetLeft + cell.offsetWidth - Math.round(splitter.offsetWidth / 2) + "px";
                    splitter.style.height = headLine.offsetHeight + "px";
                }
            };
            Table.CLASS = "tui-table";
            return Table;
        })(ctrl.Control);
        ctrl.Table = Table;

        /**
        * Construct a table control.
        * @param el {HTMLElement or element id or construct info}
        */
        function table(param) {
            return tui.ctrl.control(param, Table);
        }
        ctrl.table = table;

        tui.ctrl.registerInitCallback(Table.CLASS, table);
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;
})(tui || (tui = {}));
var tui;
(function (tui) {
    /// <reference path="tui.ctrl.control.ts" />
    (function (ctrl) {
        var Grid = (function (_super) {
            __extends(Grid, _super);
            function Grid(el) {
                _super.call(this, "div", Grid.CLASS, el);
                this._tableId = tui.uuid();
                this._gridStyle = null;
                // Grid data related
                this._columns = null;
                this._emptyColumns = [];
                this._data = null;
                this._emptyData = new tui.ArrayProvider([]);
                this._splitters = [];
                // Scrolling related
                this._scrollTop = 0;
                this._scrollLeft = 0;
                this._bufferedLines = [];
                this._bufferedBegin = 0;
                this._bufferedEnd = 0;
                this._dispLines = 0;
                // Drawing related flags
                this._selectrows = [];
                this._activerow = null;
                this._columnKeyMap = null;
                var self = this;

                this.attr("tabIndex", "0");
                this[0].innerHTML = "";
                if (document.createStyleSheet) {
                    this._gridStyle = document.createStyleSheet();
                } else {
                    this._gridStyle = document.createElement("style");
                    document.head.appendChild(this._gridStyle);
                }
                this._headline = document.createElement("div");
                this._headline.className = "tui-grid-head";
                this[0].appendChild(this._headline);
                this._hscroll = tui.ctrl.scrollbar();
                this._hscroll.direction("horizontal");
                this[0].appendChild(this._hscroll[0]);
                this._vscroll = tui.ctrl.scrollbar();
                this._vscroll.direction("vertical");
                this[0].appendChild(this._vscroll[0]);
                this._space = document.createElement("span");
                this._space.className = "tui-scroll-space";
                this[0].appendChild(this._space);

                this._vscroll.on("scroll", function (data) {
                    self._scrollTop = data["value"];
                    self.drawLines();
                });
                this._hscroll.on("scroll", function (data) {
                    self._scrollLeft = data["value"];
                    self.drawLines();
                });
                var mousewheelevt = (/Firefox/i.test(navigator.userAgent)) ? "DOMMouseScroll" : "mousewheel";
                $(this[0]).on(mousewheelevt, function (ev) {
                    var e = ev.originalEvent;
                    var delta = e.detail ? e.detail * (-120) : e.wheelDelta;
                    var step = Math.round(self._vscroll.page() / 2);

                    //delta returns +120 when wheel is scrolled up, -120 when scrolled down
                    var scrollSize = step > self._vscroll.step() ? step : self._vscroll.step();
                    if (delta <= -120) {
                        if (self._vscroll.value() < self._vscroll.total()) {
                            self._vscroll.value(self._vscroll.value() + scrollSize);
                            self._scrollTop = self._vscroll.value();
                            self.drawLines();
                            ev.stopPropagation();
                            ev.preventDefault();
                        } else if (self.consumeMouseWheelEvent()) {
                            ev.stopPropagation();
                            ev.preventDefault();
                        }
                    } else {
                        if (self._vscroll.value() > 0) {
                            self._vscroll.value(self._vscroll.value() - scrollSize);
                            self._scrollTop = self._vscroll.value();
                            self.drawLines();
                            ev.stopPropagation();
                            ev.preventDefault();
                        } else if (self.consumeMouseWheelEvent()) {
                            ev.stopPropagation();
                            ev.preventDefault();
                        }
                    }
                });
                $(this[0]).mousedown(function (e) {
                    tui.focusWithoutScroll(self[0]);
                    e.preventDefault();
                });
                $(this[0]).keyup(function (e) {
                    self.fire("keyup", { event: e });
                });
                $(this[0]).keydown(function (e) {
                    if (self.fire("keydown", { event: e }) === false)
                        return;
                    var data = self.myData();
                    var k = e.keyCode;

                    // 37:left 38:up 39:right 40:down
                    if ([33, 34, 37, 38, 39, 40].indexOf(k) >= 0) {
                        if (k === 37) {
                            !self._hscroll.hidden() && self._hscroll.value(self._hscroll.value() - self._hscroll.step());
                            self._scrollLeft = self._hscroll.value();
                            self.drawLines();
                        } else if (k === 38) {
                            if (!self.rowselectable() || data.length() <= 0) {
                                !self._vscroll.hidden() && self._vscroll.value(self._vscroll.value() - self._vscroll.step());
                                self._scrollTop = self._vscroll.value();
                                self.drawLines();
                            } else {
                                if (self._activerow === null) {
                                    self.activerow(0);
                                    self.scrollTo(self._activerow);
                                } else {
                                    if (self._activerow > 0)
                                        self.activerow(self._activerow - 1);
                                    self.scrollTo(self._activerow);
                                }
                            }
                        } else if (k === 39) {
                            !self._hscroll.hidden() && self._hscroll.value(self._hscroll.value() + self._hscroll.step());
                            self._scrollLeft = self._hscroll.value();
                            self.drawLines();
                        } else if (k === 40) {
                            if (!self.rowselectable() || data.length() <= 0) {
                                !self._vscroll.hidden() && self._vscroll.value(self._vscroll.value() + self._vscroll.step());
                                self._scrollTop = self._vscroll.value();
                                self.drawLines();
                            } else {
                                if (self._activerow === null) {
                                    self.activerow(0);
                                    self.scrollTo(self._activerow);
                                } else {
                                    if (self._activerow < data.length() - 1)
                                        self.activerow(self._activerow + 1);
                                    self.scrollTo(self._activerow);
                                }
                            }
                        } else if (k === 33) {
                            if (!self.rowselectable() || data.length() <= 0) {
                                !self._vscroll.hidden() && self._vscroll.value(self._vscroll.value() - self._vscroll.page());
                                self._scrollTop = self._vscroll.value();
                                self.drawLines();
                            } else {
                                if (self._activerow === null) {
                                    self.activerow(0);
                                    self.scrollTo(self._activerow);
                                } else {
                                    if (self._activerow > 0)
                                        self.activerow(self._activerow - self._dispLines);
                                    self.scrollTo(self._activerow);
                                }
                            }
                        } else if (k === 34) {
                            if (!self.rowselectable() || data.length() <= 0) {
                                !self._vscroll.hidden() && self._vscroll.value(self._vscroll.value() + self._vscroll.page());
                                self._scrollTop = self._vscroll.value();
                                self.drawLines();
                            } else {
                                if (self._activerow === null) {
                                    self.activerow(self._dispLines);
                                    self.scrollTo(self._activerow);
                                } else {
                                    if (self._activerow < data.length() - 1)
                                        self.activerow(self._activerow + self._dispLines);
                                    self.scrollTo(self._activerow);
                                }
                            }
                        }
                        e.preventDefault();
                        e.stopPropagation();
                        if (tui.ieVer > 0)
                            self[0].setActive();
                    }
                });
                var predefined = this.attr("data-data");
                if (predefined)
                    predefined = eval("(" + predefined + ")");
                if (predefined)
                    this.data(predefined);
                this.refresh();
            }
            // Make sure not access null object
            Grid.prototype.myData = function () {
                return this._data || this._emptyData;
            };

            Grid.prototype.myColumns = function () {
                return this.columns() || this._emptyColumns;
            };

            Grid.prototype.headHeight = function () {
                if (!this.noHead())
                    return this._headHeight;
                else
                    return 0;
            };

            Grid.colSize = function (size, def) {
                if (typeof size === "number" && !isNaN(size)) {
                    if (size < 0)
                        return 0;
                    else
                        return Math.round(size);
                } else
                    return def;
            };

            Grid.prototype.computeVScroll = function (mark) {
                var hScrollbarHeight = this._hscroll.hidden() ? 0 : this._hscroll[0].offsetHeight;
                var contentHeight = this._contentHeight;
                var innerHeight = this._boxHeight - hScrollbarHeight;
                var totalHeight = contentHeight + this.headHeight();
                this._dispLines = Math.ceil((innerHeight - this.headHeight()) / this._lineHeight);
                var vHidden = this._vscroll.hidden();
                if (totalHeight > innerHeight) {
                    this._vscroll.hidden(false);
                    this._vscroll[0].style.bottom = hScrollbarHeight + "px";
                    this._vscroll.total(totalHeight - innerHeight).value(this._scrollTop).step(this._lineHeight).page(innerHeight / totalHeight * (totalHeight - innerHeight));
                } else {
                    this._vscroll.hidden(true);
                    this._vscroll.total(0);
                }
                this._scrollTop = this._vscroll.value();
                if (vHidden !== this._vscroll.hidden()) {
                    this.computeHScroll(mark);
                    this.computeColumns();
                }
            };

            Grid.prototype.computeHScroll = function (mark) {
                mark.isHScrollComputed = true;
                var columns = this.myColumns();
                var vScrollbarWidth = this._vscroll.hidden() ? 0 : this._vscroll[0].offsetWidth;
                var innerWidth = this._boxWidth - vScrollbarWidth;
                var hHidden = this._hscroll.hidden();
                if (this.hasHScroll()) {
                    this._contentWidth = 0;
                    var cols = (columns.length < 1 ? 1 : columns.length);
                    var defaultWidth = Math.floor((innerWidth - this._borderWidth * cols) / cols);
                    for (var i = 0; i < columns.length; i++) {
                        this._contentWidth += Grid.colSize(columns[i].width, defaultWidth) + this._borderWidth;
                    }
                    if (this._contentWidth > innerWidth) {
                        this._hscroll.hidden(false);
                        this._hscroll[0].style.right = vScrollbarWidth + "px";
                        this._hscroll.total(this._contentWidth - innerWidth).value(this._scrollLeft).step(10).page(innerWidth / this._contentWidth * (this._contentWidth - innerWidth));
                    } else {
                        this._hscroll.hidden(true);
                        this._hscroll.total(0);
                    }
                } else {
                    this._contentWidth = innerWidth;
                    this._hscroll.hidden(true);
                    this._hscroll.total(0);
                }
                this._scrollLeft = this._hscroll.value();
                if (hHidden !== this._hscroll.hidden())
                    this.computeVScroll(mark);
            };

            Grid.prototype.computeScroll = function () {
                this._boxWidth = this[0].clientWidth;
                this._boxHeight = this[0].clientHeight;
                var cell = document.createElement("span");
                cell.className = "tui-grid-cell";
                var line = document.createElement("span");
                line.className = "tui-grid-line";
                line.appendChild(cell);
                cell.innerHTML = "a";
                this[0].appendChild(line);
                this._lineHeight = line.offsetHeight;
                this._borderWidth = $(cell).outerWidth() - $(cell).width();
                cell.className = "tui-grid-head-cell";
                line.className = "tui-grid-head";
                this._headHeight = line.offsetHeight;
                this[0].removeChild(line);
                this._contentHeight = this._lineHeight * this.myData().length();
                var mark = { isHScrollComputed: false };
                this._hscroll.hidden(true);
                this._vscroll.hidden(true);
                this.computeVScroll(mark);
                if (!mark.isHScrollComputed) {
                    this.computeHScroll(mark);
                    this.computeColumns();
                }
                if (!this._hscroll.hidden() && !this._vscroll.hidden()) {
                    this._space.style.display = "";
                } else
                    this._space.style.display = "none";
            };

            // Do not need call this function standalone,
            // it's always to be called by computeScroll function
            Grid.prototype.computeColumns = function () {
                var columns = this.myColumns();
                var vScrollbarWidth = this._vscroll.hidden() ? 0 : this._vscroll[0].offsetWidth;
                var innerWidth = this._boxWidth - vScrollbarWidth;
                var cols = (columns.length < 1 ? 1 : columns.length);
                var defaultWidth = Math.floor((innerWidth - this._borderWidth * cols) / cols);
                if (this.hasHScroll()) {
                    if (defaultWidth < 100)
                        defaultWidth = 100;
                    for (var i = 0; i < columns.length; i++) {
                        delete columns[i]["_important"];
                        columns[i].width = Grid.colSize(columns[i].width, defaultWidth);
                    }
                } else {
                    var totalNoBorderWidth = this._contentWidth - this._borderWidth * cols;
                    var totalNoImportantWidth = totalNoBorderWidth;
                    var totalNeedComputed = 0;
                    var important = null;
                    for (var i = 0; i < columns.length; i++) {
                        if (typeof columns[i].width !== "number" || isNaN(columns[i].width))
                            columns[i].width = defaultWidth;
                        else if (columns[i].width < 0)
                            columns[i].width = 0;
                        if (columns[i]["_important"]) {
                            important = i;
                            delete columns[i]["_important"];
                            columns[i].width = Math.round(columns[i].width);
                            if (columns[i].width > totalNoBorderWidth) {
                                columns[i].width = totalNoBorderWidth;
                            }
                            totalNoImportantWidth -= columns[i].width;
                        } else
                            totalNeedComputed += Math.round(columns[i].width);
                    }
                    for (var i = 0; i < columns.length; i++) {
                        if (i !== important) {
                            if (totalNeedComputed === 0)
                                columns[i].width = 0;
                            else
                                columns[i].width = Math.floor(Math.round(columns[i].width) / totalNeedComputed * totalNoImportantWidth);
                        }
                    }
                    var total = 0;
                    for (var i = 0; i < columns.length; i++) {
                        total += columns[i].width;
                    }
                    if (total < totalNoBorderWidth && columns.length > 0)
                        columns[columns.length - 1].width += totalNoBorderWidth - total;
                }
                var cssText = "";
                for (var i = 0; i < columns.length; i++) {
                    var wd = columns[i].width;
                    cssText += (".tui-grid-" + this._tableId + "-" + i + "{width:" + wd + "px}");
                }
                if (document.createStyleSheet)
                    this._gridStyle.cssText = cssText;
                else
                    this._gridStyle.innerHTML = cssText;
            };

            Grid.prototype.bindSplitter = function (cell, col, colIndex) {
                var self = this;
                var splitter = document.createElement("span");
                splitter.className = "tui-grid-splitter";
                splitter.setAttribute("unselectable", "on");
                $(splitter).mousedown(function (e) {
                    var l = splitter.offsetLeft;
                    var srcX = e.clientX;
                    splitter.style.height = self[0].clientHeight + "px";
                    splitter.style.bottom = "";
                    $(splitter).addClass("tui-splitter-move");
                    var mask = tui.mask();
                    mask.style.cursor = "col-resize";
                    function onDragEnd(e) {
                        $(document).off("mousemove", onDrag);
                        $(document).off("mouseup", onDragEnd);
                        tui.unmask();
                        splitter.style.bottom = "0";
                        splitter.style.height = "";
                        $(splitter).removeClass("tui-splitter-move");
                        col.width = col.width + e.clientX - srcX;
                        col["_important"] = true;
                        var currentTime = tui.today().getTime();
                        if (col["_lastClickTime"]) {
                            if (currentTime - col["_lastClickTime"] < 500) {
                                self.autofitColumn(colIndex, false, true);
                                self.fire("resizecolumn", { col: colIndex });
                                return;
                            }
                        }
                        col["_lastClickTime"] = currentTime;
                        self.refresh();
                        self.fire("resizecolumn", { col: colIndex });
                    }
                    function onDrag(e) {
                        splitter.style.left = l + e.clientX - srcX + "px";
                    }
                    $(document).on("mousemove", onDrag);
                    $(document).on("mouseup", onDragEnd);
                });
                this._splitters.push(splitter);
                return splitter;
            };

            Grid.prototype.bindSort = function (cell, col, colIndex) {
                var self = this;
                if (col.sort) {
                    $(cell).addClass("tui-grid-sortable");
                    $(cell).mousedown(function (event) {
                        if (!tui.isLButton(event.button))
                            return;
                        if (self._sortColumn !== colIndex)
                            self.sort(colIndex);
                        else if (!self._sortDesc)
                            self.sort(colIndex, true);
                        else
                            self.sort(null);
                    });
                }
                if (self._sortColumn === colIndex) {
                    if (self._sortDesc)
                        $(cell).addClass("tui-grid-cell-sort-desc");
                    else
                        $(cell).addClass("tui-grid-cell-sort-asc");
                }
            };

            Grid.prototype.moveSplitter = function () {
                for (var i = 0; i < this._splitters.length; i++) {
                    var splitter = this._splitters[i];
                    var cell = this._headline.childNodes[i];
                    splitter.style.left = cell.offsetLeft + cell.offsetWidth - Math.round(splitter.offsetWidth / 2) + "px";
                }
            };

            Grid.prototype.drawCell = function (cell, contentSpan, col, value, row, rowIndex, colIndex) {
                if (rowIndex >= 0) {
                    if (["center", "left", "right"].indexOf(col.align) >= 0)
                        cell.style.textAlign = col.align;
                } else {
                    if (["center", "left", "right"].indexOf(col.headAlign) >= 0)
                        cell.style.textAlign = col.headAlign;
                }
                if (value === null) {
                    contentSpan.innerHTML = "";
                } else if (typeof value === "object" && value.nodeName) {
                    contentSpan.innerHTML = "";
                    contentSpan.appendChild(value);
                } else {
                    contentSpan.innerHTML = value;
                }
                if (typeof col.format === "function") {
                    col.format.call(this, {
                        cell: cell,
                        value: value,
                        row: row,
                        rowIndex: rowIndex,
                        colIndex: colIndex
                    });
                }
                if (this._sortColumn === colIndex)
                    $(cell).addClass("tui-grid-sort-cell");
                else
                    $(cell).removeClass("tui-grid-sort-cell");
            };

            Grid.prototype.drawHead = function () {
                if (this.noHead()) {
                    $(this._headline).addClass("tui-hidden");
                    return;
                }
                $(this._headline).removeClass("tui-hidden");
                var columns = this.myColumns();
                this._headline.innerHTML = "";
                this._splitters.length = 0;
                for (var i = 0; i < columns.length; i++) {
                    var col = columns[i];
                    var cell = document.createElement("span");
                    cell.setAttribute("unselectable", "on");
                    cell.className = "tui-grid-head-cell tui-grid-" + this._tableId + "-" + i;
                    this._headline.appendChild(cell);
                    var contentSpan = document.createElement("span");
                    contentSpan.className = "tui-grid-cell-content";
                    cell.appendChild(contentSpan);
                    this.drawCell(cell, contentSpan, col, col.name, null, -1, i);
                    this.bindSort(cell, col, i);
                    if (this.resizable()) {
                        var splitter = this.bindSplitter(cell, col, i);
                        if (typeof columns[i].fixed === "boolean" && columns[i].fixed)
                            $(splitter).addClass("tui-hidden");
                    }
                }
                for (var i = 0; i < this._splitters.length; i++) {
                    var splitter = this._splitters[i];
                    this._headline.appendChild(splitter);
                }
                this.moveSplitter();
            };

            Grid.prototype.isRowSelected = function (rowIndex) {
                return this._selectrows.indexOf(rowIndex) >= 0;
            };

            Grid.prototype.drawLine = function (line, index, bindEvent) {
                if (typeof bindEvent === "undefined") { bindEvent = false; }
                var self = this;
                var columns = this.myColumns();
                var data = this.myData();
                var rowData = data.at(index);
                if (line.childNodes.length !== columns.length) {
                    line.innerHTML = "";
                    for (var i = 0; i < columns.length; i++) {
                        var cell = document.createElement("span");
                        if (this.rowselectable())
                            cell.setAttribute("unselectable", "on");
                        cell.className = "tui-grid-cell tui-grid-" + this._tableId + "-" + i;
                        var contentSpan = document.createElement("span");
                        contentSpan.className = "tui-grid-cell-content";
                        cell.appendChild(contentSpan);
                        line.appendChild(cell);
                    }
                }
                for (var i = 0; i < line.childNodes.length; i++) {
                    var cell = line.childNodes[i];
                    var col = columns[i];
                    var key = null;
                    if (typeof col.key !== tui.undef && col.key !== null) {
                        key = this._columnKeyMap[col.key];
                        if (typeof key === tui.undef)
                            key = col.key;
                    }
                    var value = (key !== null && rowData ? rowData[key] : " ");
                    this.drawCell(cell, cell.firstChild, col, value, rowData, index, i);
                }

                if (!bindEvent)
                    return;
                $(line).on("contextmenu", function (e) {
                    var index = line["_rowIndex"];
                    self.fire("rowcontextmenu", { "event": e, "index": index, "row": line });
                });
                $(line).mousedown(function (e) {
                    var index = line["_rowIndex"];
                    if (self.rowselectable())
                        self.activerow(index);
                    self.fire("rowmousedown", { "event": e, "index": index, "row": line });
                });
                $(line).mouseup(function (e) {
                    var index = line["_rowIndex"];
                    self.fire("rowmouseup", { "event": e, "index": index, "row": line });
                });
                $(line).on("click", function (e) {
                    var index = line["_rowIndex"];
                    self.fire("rowclick", { "event": e, "index": index, "row": line });
                });
                $(line).on("dblclick", function (e) {
                    var index = line["_rowIndex"];
                    self.fire("rowdblclick", { "event": e, "index": index, "row": line });
                });
            };

            Grid.prototype.moveLine = function (line, index, base) {
                line.style.top = (base + index * this._lineHeight) + "px";
                line.style.left = -this._scrollLeft + "px";
            };

            Grid.prototype.drawLines = function () {
                this._headline.style.left = -this._scrollLeft + "px";
                var base = this.headHeight() - this._scrollTop % this._lineHeight;
                var begin = Math.floor(this._scrollTop / this._lineHeight);
                var newBuffer = [];
                var data = this.myData();
                for (var i = begin; i < begin + this._dispLines + 1 && i < data.length(); i++) {
                    if (i >= this._bufferedBegin && i < this._bufferedEnd) {
                        // Is buffered.
                        var line = this._bufferedLines[i - this._bufferedBegin];
                        this.moveLine(line, i - begin, base);
                        newBuffer.push(line);
                    } else {
                        var line = document.createElement("div");
                        line.className = "tui-grid-line";
                        this[0].insertBefore(line, this._headline);
                        newBuffer.push(line);
                        line["_rowIndex"] = i;
                        this.drawLine(line, i, true);
                        this.moveLine(line, i - begin, base);
                    }
                    if (this.isRowSelected(i)) {
                        $(line).addClass("tui-grid-line-selected");
                    } else
                        $(line).removeClass("tui-grid-line-selected");
                }
                var end = i;
                for (var i = this._bufferedBegin; i < this._bufferedEnd; i++) {
                    if (i < begin || i >= end)
                        this[0].removeChild(this._bufferedLines[i - this._bufferedBegin]);
                }
                this._bufferedLines = newBuffer;
                this._bufferedBegin = begin;
                this._bufferedEnd = end;
            };

            Grid.prototype.clearBufferLines = function () {
                if (!this[0])
                    return;
                for (var i = 0; i < this._bufferedLines.length; i++) {
                    var l = this._bufferedLines[i];
                    this[0].removeChild(l);
                }
                this._bufferedLines = [];
                this._bufferedEnd = this._bufferedBegin = 0;
            };

            Grid.prototype.lineHeight = function () {
                return this._lineHeight;
            };

            Grid.prototype.select = function (rows) {
                if (rows && typeof rows.length === "number" && rows.length >= 0) {
                    this._selectrows.length = 0;
                    for (var i = 0; i < rows.length; i++) {
                        this._selectrows.push(rows[i]);
                    }

                    // Clear buffer cause row click event cannot be raised,
                    // so never do this when we only want to change row selection status.
                    // this.clearBufferLines();
                    this.drawLines();
                }
                return this._selectrows;
            };

            Grid.prototype.activerow = function (rowIndex) {
                if (typeof rowIndex === "number" || rowIndex === null) {
                    if (rowIndex < 0)
                        rowIndex = 0;
                    if (rowIndex >= this.myData().length())
                        rowIndex = this.myData().length() - 1;
                    this._activerow = rowIndex;
                    if (rowIndex === null)
                        this.select([]);
                    else
                        this.select([rowIndex]);
                }
                return this._activerow;
            };

            Grid.prototype.activeItem = function (rowItem) {
                var data = this.myData();
                if (typeof rowItem !== tui.undef) {
                    if (rowItem === null) {
                        this.activerow(null);
                    } else {
                        for (var i = 0; i < data.length(); i++) {
                            if (data.at(i) === rowItem) {
                                this.activerow(i);
                                break;
                            }
                        }
                    }
                }
                if (this._activerow !== null) {
                    return data.at(this._activerow);
                } else
                    return null;
            };

            /**
            * Sort by specifed column
            * @param {Number} colIndex
            * @param {Boolean} desc
            */
            Grid.prototype.sort = function (colIndex, desc) {
                if (typeof desc === "undefined") { desc = false; }
                var columns = this.myColumns();
                if (colIndex === null) {
                    this._sortColumn = null;
                    this.myData().sort(null, desc);
                    this._sortDesc = false;
                } else if (typeof colIndex === "number" && !isNaN(colIndex) && colIndex >= 0 && colIndex < columns.length && columns[colIndex].sort) {
                    this._sortColumn = colIndex;
                    this._sortDesc = desc;
                    if (typeof columns[colIndex].sort === "function")
                        this.myData().sort(columns[colIndex].key, this._sortDesc, columns[colIndex].sort);
                    else
                        this.myData().sort(columns[colIndex].key, this._sortDesc);
                }
                this._sortDesc = !!desc;
                this._scrollTop = 0;
                this.activerow(null);
                this.refresh();
                return { colIndex: this._sortColumn, desc: this._sortDesc };
            };

            /**
            * Adjust column width to adapt column content
            * @param {Number} columnIndex
            * @param {Boolean} expandOnly only expand column width
            */
            Grid.prototype.autofitColumn = function (columnIndex, expandOnly, displayedOnly) {
                if (typeof expandOnly === "undefined") { expandOnly = false; }
                if (typeof displayedOnly === "undefined") { displayedOnly = true; }
                if (typeof (columnIndex) !== "number")
                    return;
                var columns = this.myColumns();
                if (columnIndex < 0 && columnIndex >= columns.length)
                    return;
                var col = columns[columnIndex];
                var maxWidth = 0;
                if (expandOnly)
                    maxWidth = col.width || 0;
                var cell = document.createElement("span");
                cell.className = "tui-grid-cell";
                cell.style.position = "absolute";
                cell.style.visibility = "hidden";
                cell.style.width = "auto";
                document.body.appendChild(cell);
                var key = columnIndex;
                if (typeof col.key === "string" && col.key || typeof col.key === "number" && !isNaN(col.key))
                    key = col.key;
                var data = this.myData();
                var begin = displayedOnly ? this._bufferedBegin : 0;
                var end = displayedOnly ? this._bufferedEnd : data.length();
                for (var i = begin; i < end; i++) {
                    var rowData = data.at(i);
                    var v = rowData[key];
                    if (typeof v === "object" && v.nodeName) {
                        cell.innerHTML = "";
                        cell.appendChild(v);
                    } else {
                        cell.innerHTML = v;
                    }
                    if (typeof col.format === "function")
                        col.format({
                            cell: cell,
                            value: v,
                            row: rowData,
                            rowIndex: i,
                            colIndex: columnIndex
                        });
                    if (maxWidth < cell.offsetWidth - this._borderWidth)
                        maxWidth = cell.offsetWidth - this._borderWidth;
                }
                document.body.removeChild(cell);
                col.width = maxWidth;
                col["_important"] = true;
                this.refresh();
            };

            Grid.prototype.hasHScroll = function (val) {
                if (typeof val === "boolean") {
                    this.is("data-has-hscroll", val);
                    this.refresh();
                    return this;
                } else
                    return this.is("data-has-hscroll");
            };

            Grid.prototype.noHead = function (val) {
                if (typeof val === "boolean") {
                    this.is("data-no-head", val);
                    this.refresh();
                    return this;
                } else
                    return this.is("data-no-head");
            };

            Grid.prototype.columns = function (val) {
                if (val) {
                    this._columns = val;
                    this.refresh();
                    return this;
                } else {
                    if (!this._columns) {
                        var valstr = this.attr("data-columns");
                        this._columns = eval("(" + valstr + ")");
                    }
                    return this._columns;
                }
            };

            Grid.prototype.rowselectable = function (val) {
                if (typeof val === "boolean") {
                    this.is("data-rowselectable", val);
                    this.refresh();
                    return this;
                } else
                    return this.is("data-rowselectable");
            };

            Grid.prototype.scrollTo = function (rowIndex) {
                if (typeof rowIndex !== "number" || isNaN(rowIndex) || rowIndex < 0 || rowIndex >= this.myData().length())
                    return;
                var v = this._vscroll.value();
                if (v > rowIndex * this._lineHeight) {
                    this._vscroll.value(rowIndex * this._lineHeight);
                    this._scrollTop = this._vscroll.value();
                    this.drawLines();
                } else {
                    var h = (rowIndex - this._dispLines + 1) * this._lineHeight;
                    var diff = (this._boxHeight - this.headHeight() - this._hscroll[0].offsetHeight - this._dispLines * this._lineHeight);
                    if (v < h - diff) {
                        this._vscroll.value(h - diff);
                        this._scrollTop = this._vscroll.value();
                        this.drawLines();
                    }
                }
            };

            Grid.prototype.resizable = function (val) {
                if (typeof val === "boolean") {
                    this.is("data-resizable", val);
                    this.refresh();
                    return this;
                } else
                    return this.is("data-resizable");
            };

            Grid.prototype.value = function (data) {
                if (data === null) {
                    return this.data([]);
                } else if (data) {
                    return this.data(data);
                } else {
                    var result = [];
                    var dt = this.data();
                    for (var i = 0; i < dt.length(); i++) {
                        result.push(dt.at(i));
                    }
                    return result;
                }
            };

            Grid.prototype.data = function (data) {
                if (data) {
                    var self = this;
                    if (data instanceof Array || data.data && data.data instanceof Array) {
                        data = new tui.ArrayProvider(data);
                    }
                    if (typeof data.length !== "function" || typeof data.sort !== "function" || typeof data.at !== "function" || typeof data.columnKeyMap !== "function") {
                        throw new Error("TUI Grid: need a data provider.");
                    }
                    this._data && this._data.onupdate && this._data.onupdate(null);
                    this._data = data;
                    typeof this._data.onupdate === "function" && this._data.onupdate(function (updateInfo) {
                        var b = updateInfo.begin;
                        var e = b + updateInfo.data.length;
                        self.refresh();
                    });
                    this.refresh();
                    return this;
                } else {
                    return this._data;
                }
            };

            Grid.prototype.consumeMouseWheelEvent = function (val) {
                if (typeof val === "boolean") {
                    this.is("data-consume-mwe", val);
                    this.refresh();
                    return this;
                } else
                    return this.is("data-consume-mwe");
            };

            Grid.prototype.refresh = function () {
                if (!this[0])
                    return;
                this.computeScroll();
                this.clearBufferLines();
                this._columnKeyMap = this.myData().columnKeyMap();
                this.drawHead();
                this.drawLines();
            };
            Grid.CLASS = "tui-grid";
            return Grid;
        })(ctrl.Control);
        ctrl.Grid = Grid;

        /**
        * Construct a grid.
        * @param el {HTMLElement or element id or construct info}
        */
        function grid(param) {
            return tui.ctrl.control(param, Grid);
        }
        ctrl.grid = grid;

        tui.ctrl.registerInitCallback(Grid.CLASS, grid);
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;
})(tui || (tui = {}));
var tui;
(function (tui) {
    /// <reference path="tui.ctrl.control.ts" />
    (function (ctrl) {
        var TriState;
        (function (TriState) {
            TriState[TriState["Unchecked"] = 0] = "Unchecked";
            TriState[TriState["HalfChecked"] = 1] = "HalfChecked";
            TriState[TriState["Checked"] = 2] = "Checked";
        })(TriState || (TriState = {}));
        var List = (function (_super) {
            __extends(List, _super);
            function List(el) {
                var _this = this;
                _super.call(this, "div", List.CLASS, null);
                this._columnKeyMap = null;
                var self = this;
                this._grid = ctrl.grid(el);
                this[0] = this._grid[0];
                this[0]._ctrl = this;
                this.addClass(List.CLASS);
                this._grid.noHead(true);
                var columns = this._grid.columns();
                if (columns === null) {
                    this._grid.columns([{
                            key: "value",
                            format: function (info) {
                                var rowcheckable = _this.rowcheckable();
                                var cell = info.cell.firstChild;
                                var isExpanded = !!info.row[_this._expandColumnKey];
                                var hasCheckbox = (typeof info.row[_this._checkedColumnKey] !== tui.undef);
                                var isChecked = !!info.row[_this._checkedColumnKey];
                                var hasChild = !!info.row[_this._childrenColumKey];
                                var isHalfChecked = (_this.triState() && info.row[_this._checkedColumnKey] === 1 /* HalfChecked */);
                                var spaceSpan = document.createElement("span");

                                spaceSpan.className = "tui-list-space";
                                var foldIcon = document.createElement("span");
                                foldIcon.className = "tui-list-fold";
                                if (hasChild) {
                                    if (isExpanded) {
                                        $(foldIcon).addClass("tui-list-fold-expand");
                                        $(foldIcon).mousedown(function (e) {
                                            _this.onFoldRow(info.row, info.rowIndex, e);
                                        });
                                    } else {
                                        $(foldIcon).addClass("tui-list-fold-unexpand");
                                        $(foldIcon).mousedown(function (e) {
                                            _this.onExpandRow(info.row, info.rowIndex, e);
                                        });
                                    }
                                }

                                if (hasCheckbox && rowcheckable) {
                                    var checkIcon = document.createElement("span");
                                    checkIcon.className = "tui-list-checkbox";
                                    if (isChecked) {
                                        if (isHalfChecked)
                                            $(checkIcon).addClass("tui-half-checked");
                                        else
                                            $(checkIcon).addClass("tui-checked");
                                    }
                                    cell.insertBefore(checkIcon, cell.firstChild);
                                    $(checkIcon).mouseup(function (e) {
                                        _this.onCheckRow(info.row, info.rowIndex, e);
                                    });
                                }
                                cell.insertBefore(foldIcon, cell.firstChild);
                                cell.insertBefore(spaceSpan, cell.firstChild);
                                var singleWidth = spaceSpan.offsetWidth;
                                var level = info.row[_this._levelColumnKey];
                                spaceSpan.style.width = singleWidth * (typeof level === "number" ? level : 0) + "px";
                            }
                        }]);
                }
                this._grid.on("rowclick", function (data) {
                    _this.fire("rowclick", data);
                });
                this._grid.on("rowdblclick", function (data) {
                    _this.fire("rowdblclick", data);
                });
                this._grid.on("rowmousedown", function (data) {
                    _this.fire("rowmousedown", data);
                });
                this._grid.on("rowmouseup", function (data) {
                    _this.fire("rowmouseup", data);
                });
                this._grid.on("rowcontextmenu", function (data) {
                    _this.fire("rowcontextmenu", data);
                });
                this._grid.on("resizecolumn", function (data) {
                    _this.fire("resizecolumn", data);
                });
                this._grid.on("keydown", function (data) {
                    var keyCode = data["event"].keyCode;
                    if (keyCode === 32) {
                        var activeRowIndex = self._grid.activerow();
                        if (activeRowIndex >= 0) {
                            data["event"].preventDefault();
                            data["event"].stopPropagation();
                        }
                    } else if (keyCode === 37) {
                        var item = self._grid.activeItem();
                        if (item) {
                            var children = item[self._childrenColumKey];
                            if (children && children.length > 0 && item[self._expandColumnKey]) {
                                item[self._expandColumnKey] = false;
                                _this.formatData();
                            } else {
                                if (item["__parent"]) {
                                    self.activeItem(item["__parent"]);
                                    self.scrollTo(self.activerow());
                                    self.refresh();
                                }
                            }

                            data["event"].preventDefault();
                            data["event"].stopPropagation();
                        }
                    } else if (keyCode === 39) {
                        var item = self._grid.activeItem();
                        if (item) {
                            var children = item[self._childrenColumKey];
                            if (children && children.length > 0 && !item[self._expandColumnKey]) {
                                item[self._expandColumnKey] = true;
                                _this.formatData();
                            }
                            data["event"].preventDefault();
                            data["event"].stopPropagation();
                        }
                    }
                    _this.fire("keydown", data);
                });
                this._grid.on("keyup", function (data) {
                    if (data["event"].keyCode === 32) {
                        var activeRowIndex = self._grid.activerow();
                        if (activeRowIndex >= 0) {
                            var row = self._grid.data().at(activeRowIndex);
                            _this.onCheckRow(row, activeRowIndex, data["event"]);
                        }
                    }
                    _this.fire("keyup", data);
                });

                if (!this.hasAttr("data-rowselectable"))
                    this.rowselectable(true);
                if (!this.hasAttr("data-rowcheckable"))
                    this.rowcheckable(true);
                if (this._grid.data()) {
                    this.data(this._grid.data());
                }
            }
            List.prototype.checkChildren = function (children, checkState) {
                for (var i = 0; i < children.length; i++) {
                    if (!children[i])
                        continue;
                    children[i][this._checkedColumnKey] = checkState;
                    var myChildren = children[i][this._childrenColumKey];
                    myChildren && myChildren.length > 0 && this.checkChildren(myChildren, checkState);
                }
            };

            List.prototype.checkParent = function (parent) {
                var children = parent[this._childrenColumKey];
                var checkedCount = 0, uncheckedCount = 0;
                for (var i = 0; i < children.length; i++) {
                    var row = children[i];
                    if (!row)
                        continue;
                    if (row[this._checkedColumnKey] === 1 /* HalfChecked */) {
                        uncheckedCount++;
                        checkedCount++;
                        break;
                    } else if (!!row[this._checkedColumnKey])
                        checkedCount++;
                    else
                        uncheckedCount++;
                }
                if (checkedCount === 0)
                    parent[this._checkedColumnKey] = 0 /* Unchecked */;
                else if (uncheckedCount === 0)
                    parent[this._checkedColumnKey] = 2 /* Checked */;
                else
                    parent[this._checkedColumnKey] = 1 /* HalfChecked */;
                parent["__parent"] && this.checkParent(parent["__parent"]);
            };

            List.prototype.checkRow = function (row, checkState) {
                row[this._checkedColumnKey] = checkState;
                if (this.triState()) {
                    var children = row[this._childrenColumKey];
                    children && children.length > 0 && this.checkChildren(children, checkState);
                    var parent = row["__parent"];
                    parent && this.checkParent(parent);
                }
            };

            List.prototype.onCheckRow = function (row, rowIndex, event) {
                var checkState;
                if (this.triState()) {
                    checkState = row[this._checkedColumnKey];
                    if (checkState === 1 /* HalfChecked */ || !checkState)
                        checkState = 2 /* Checked */;
                    else
                        checkState = 0 /* Unchecked */;
                } else
                    checkState = !row[this._checkedColumnKey];
                this.checkRow(row, checkState);
                this.fire("rowcheck", { event: event, checked: row[this._checkedColumnKey], row: row, index: rowIndex });
                this.refresh();
            };

            List.prototype.onExpandRow = function (row, rowIndex, event) {
                row[this._expandColumnKey] = true;
                this.fire("rowexpand", { event: event, row: row, index: rowIndex });
                this.formatData();
            };

            List.prototype.onFoldRow = function (row, rowIndex, event) {
                row[this._expandColumnKey] = false;
                this.fire("rowfold", { event: event, row: row, index: rowIndex });
                this.formatData();
            };

            List.prototype.columnKey = function (key) {
                var val = this._columnKeyMap[key];
                if (typeof val === "number" && val >= 0)
                    return val;
                else
                    return key;
            };

            List.prototype.initData = function (useTriState) {
                if (typeof useTriState === "undefined") { useTriState = false; }
                var self = this;
                var data = this._grid.data();
                if (typeof data.process === "function") {
                    function checkChildren(input, parentRow) {
                        var checkedCount = 0, uncheckedCount = 0;
                        for (var i = 0; i < input.length; i++) {
                            var row = input[i];
                            if (!row)
                                continue;
                            if (useTriState) {
                                if (row[self._childrenColumKey] && row[self._childrenColumKey].length > 0) {
                                    var state = checkChildren(row[self._childrenColumKey], row);
                                    row[self._checkedColumnKey] = state;
                                }
                                if (row[self._checkedColumnKey] === 1 /* HalfChecked */) {
                                    uncheckedCount++;
                                    checkedCount++;
                                } else if (!!row[self._checkedColumnKey])
                                    checkedCount++;
                                else
                                    uncheckedCount++;
                            } else {
                                if (row[self._childrenColumKey] && row[self._childrenColumKey].length > 0) {
                                    checkChildren(row[self._childrenColumKey], row);
                                }
                            }
                            row["__parent"] = parentRow;
                        }
                        if (useTriState) {
                            if (checkedCount === 0)
                                return 0 /* Unchecked */;
                            else if (uncheckedCount === 0)
                                return 2 /* Checked */;
                            else
                                return 1 /* HalfChecked */;
                        }
                    }

                    function processTree(input) {
                        checkChildren(input, null);
                        return input;
                    }
                    data.process(processTree);
                }
            };

            List.prototype.initTriState = function () {
                this.initData(true);
            };

            List.prototype.formatData = function () {
                var self = this;
                var data = this._grid.data();
                if (typeof data.process === "function") {
                    function addChildren(input, output, level) {
                        for (var i = 0; i < input.length; i++) {
                            var row = input[i];
                            if (!row)
                                continue;
                            output.push(row);
                            row[self._levelColumnKey] = level;
                            if (!!row[self._expandColumnKey] && row[self._childrenColumKey] && row[self._childrenColumKey].length > 0) {
                                addChildren(row[self._childrenColumKey], output, level + 1);
                            }
                        }
                    }

                    function processTree(input) {
                        var output = [];
                        addChildren(input, output, 0);
                        return output;
                    }
                    data.process(processTree);
                }
                this.refresh();
            };

            List.prototype.select = function (rows) {
                return this._grid.select(rows);
            };

            List.prototype.activerow = function (rowIndex) {
                return this._grid.activerow(rowIndex);
            };

            List.prototype.activeItem = function (rowItem) {
                if (typeof rowItem !== tui.undef) {
                    if (rowItem) {
                        var parent = rowItem["__parent"];
                        while (parent) {
                            parent[this._expandColumnKey] = true;
                            parent = parent["__parent"];
                        }
                        this.formatData();
                    }
                }
                return this._grid.activeItem(rowItem);
            };

            List.prototype.activeRowByKey = function (key) {
                var self = this;
                var activeRow = null;
                function checkChildren(children) {
                    for (var i = 0; i < children.length; i++) {
                        if (!children[i])
                            continue;
                        if (children[i][self._keyColumKey] === key) {
                            activeRow = children[i];
                            return true;
                        }
                        var myChilren = children[i][self._childrenColumKey];
                        if (myChilren && myChilren.length > 0)
                            if (checkChildren(myChilren))
                                return true;
                    }
                }
                var data = this._grid.data();
                if (typeof data.src === "function") {
                    if (checkChildren(data.src())) {
                        return this.activeItem(activeRow);
                    } else
                        return this.activeItem(null);
                } else
                    return null;
            };

            List.prototype.doCheck = function (keys, checkState) {
                var self = this;

                //var useTriState = this.triState();
                var map = {};
                if (keys) {
                    for (var i = 0; i < keys.length; i++) {
                        map[keys[i]] = true;
                    }
                }
                function checkChildren(keys, children) {
                    for (var i = 0; i < children.length; i++) {
                        if (!children[i])
                            continue;
                        if (keys === null || map[children[i][self._keyColumKey]]) {
                            //children[i][self._checkedColumnKey] = checkState;
                            self.checkRow(children[i], checkState);
                        }
                        var myChilren = children[i][self._childrenColumKey];
                        if (myChilren && myChilren.length > 0)
                            checkChildren(keys, myChilren);
                    }
                }
                var data = this._grid.data();
                if (data && typeof data.src === "function") {
                    checkChildren(keys, data.src());

                    //if (useTriState) {
                    //	this.initTriState();
                    //}
                    this.refresh();
                }
            };

            List.prototype.checkItems = function (keys) {
                this.doCheck(keys, 2 /* Checked */);
                return this;
            };
            List.prototype.checkAllItems = function () {
                this.doCheck(null, 2 /* Checked */);
                return this;
            };
            List.prototype.uncheckItems = function (keys) {
                this.doCheck(keys, 0 /* Unchecked */);
                return this;
            };
            List.prototype.uncheckAllItems = function () {
                this.doCheck(null, 0 /* Unchecked */);
                return this;
            };
            List.prototype.checkedItems = function () {
                var self = this;
                var checkedItems = [];
                function checkChildren(children) {
                    for (var i = 0; i < children.length; i++) {
                        if (!children[i])
                            continue;
                        if (!!children[i][self._checkedColumnKey])
                            checkedItems.push(children[i]);
                        var myChilren = children[i][self._childrenColumKey];
                        if (myChilren && myChilren.length > 0)
                            checkChildren(myChilren);
                    }
                }
                var data = this._grid.data();
                if (data && typeof data.src === "function") {
                    checkChildren(data.src());
                }
                return checkedItems;
            };

            /**
            * Adjust column width to adapt column content
            * @param {Number} columnIndex
            * @param {Boolean} expandOnly only expand column width
            */
            List.prototype.autofitColumn = function (columnIndex, expandOnly, displayedOnly) {
                if (typeof expandOnly === "undefined") { expandOnly = false; }
                if (typeof displayedOnly === "undefined") { displayedOnly = true; }
                this._grid.autofitColumn(columnIndex, expandOnly, displayedOnly);
            };

            List.prototype.hasHScroll = function (val) {
                return this._grid.hasHScroll(val);
            };

            List.prototype.columns = function (val) {
                return this._grid.columns(val);
            };

            List.prototype.rowselectable = function (val) {
                return this._grid.rowselectable(val);
            };

            List.prototype.rowcheckable = function (val) {
                if (typeof val === "boolean") {
                    this.is("data-rowcheckable", val);
                    this.refresh();
                    return this;
                } else
                    return this.is("data-rowcheckable");
            };

            List.prototype.consumeMouseWheelEvent = function (val) {
                return this._grid.consumeMouseWheelEvent(val);
            };

            List.prototype.triState = function (val) {
                if (typeof val === "boolean") {
                    this.is("data-tri-state", val);
                    this.refresh();
                    return this;
                } else
                    return this.is("data-tri-state");
            };

            List.prototype.scrollTo = function (rowIndex) {
                this._grid.scrollTo(rowIndex);
            };

            List.prototype.value = function (keys) {
                if (typeof keys !== tui.undef) {
                    this.uncheckAllItems();
                    if (keys != null)
                        this.checkItems(keys);
                    return this;
                } else {
                    var items = this.checkedItems();
                    var result = [];
                    for (var i = 0; i < items.length; i++) {
                        if (typeof items[i].key !== tui.undef)
                            result.push(items[i].key);
                    }
                    return result;
                }
            };

            List.prototype.data = function (data) {
                var ret = this._grid.data(data);
                if (data) {
                    var data = this._grid.data();
                    if (data)
                        this._columnKeyMap = data.columnKeyMap();
                    else
                        this._columnKeyMap = {};
                    this._keyColumKey = this.columnKey("key");
                    this._childrenColumKey = this.columnKey("children");
                    this._checkedColumnKey = this.columnKey("checked");
                    this._levelColumnKey = this.columnKey("level");
                    this._valueColumnKey = this.columnKey("value");
                    this._expandColumnKey = this.columnKey("expand");
                    if (this.triState())
                        this.initTriState();
                    else
                        this.initData();
                    this.formatData();
                }
                return ret;
            };

            List.prototype.lineHeight = function () {
                if (!this._grid)
                    return 0;
                return this._grid.lineHeight();
            };

            List.prototype.refresh = function () {
                if (!this._grid)
                    return;
                this._grid.refresh();
            };
            List.CLASS = "tui-list";
            return List;
        })(ctrl.Control);
        ctrl.List = List;

        /**
        * Construct a grid.
        * @param el {HTMLElement or element id or construct info}
        */
        function list(param) {
            return tui.ctrl.control(param, List);
        }
        ctrl.list = list;

        tui.ctrl.registerInitCallback(List.CLASS, list);
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;
})(tui || (tui = {}));
var tui;
(function (tui) {
    /// <reference path="tui.core.ts" />
    /// <reference path="tui.upload.ts" />
    /// <reference path="tui.ctrl.popup.ts" />
    /// <reference path="tui.ctrl.calendar.ts" />
    /// <reference path="tui.ctrl.form.ts" />
    (function (_ctrl) {
        function validText(t) {
            if (typeof t === tui.undef || t === null) {
                return "";
            } else {
                return t + "";
            }
        }
        function getKeys(items) {
            var keys = [];
            for (var i = 0; i < items.length; i++) {
                var key = items[i]["key"];
                if (typeof key !== tui.undef)
                    keys.push(key);
            }
            return keys;
        }
        var Input = (function (_super) {
            __extends(Input, _super);
            function Input(el, type) {
                var _this = this;
                _super.call(this, "span", Input.CLASS, el);
                this._fileId = null;
                this._binding = null;
                this._invalid = false;
                this._message = "";
                this._data = null;
                this._columnKeyMap = null;
                var self = this;

                if (typeof type !== tui.undef)
                    this.type(type);

                this._button = document.createElement("span");
                this._label = document.createElement("label");
                this._notify = document.createElement("div");
                this[0].innerHTML = "";
                this[0].appendChild(this._label);
                this[0].appendChild(this._button);
                this[0].appendChild(this._notify);

                this.createTextbox();

                var openPopup = function (e) {
                    if (_this.type() === "calendar") {
                        var pop = tui.ctrl.popup();
                        var calendar = tui.ctrl.calendar();
                        calendar.time(self.value());
                        calendar.on("picked", function (e) {
                            if (self.readonly()) {
                                pop.close();
                                self.focus();
                                return false;
                            }
                            self.value(e["time"]);
                            pop.close();
                            self.focus();
                            if (self.fire("select", { ctrl: self[0], type: self.type(), time: e["time"] }) === false)
                                return;
                            self.doSubmit();
                        });
                        var calbox = document.createElement("div");
                        calbox.appendChild(calendar[0]);
                        var todayLink = document.createElement("a");
                        todayLink.innerHTML = "<i class='fa fa-clock-o'></i> " + tui.str("Today") + ": " + tui.formatDate(tui.today(), "yyyy-MM-dd");
                        todayLink.href = "javascript:void(0)";
                        $(todayLink).click(function (e) {
                            if (self.readonly()) {
                                pop.close();
                                self.focus();
                                return false;
                            }
                            self.value(tui.today());
                            pop.close();
                            self.focus();
                            if (self.fire("select", { ctrl: self[0], type: self.type(), time: e["time"] }) === false)
                                return;
                            self.doSubmit();
                        });
                        var todayLine = document.createElement("div");
                        todayLine.appendChild(todayLink);
                        todayLine.className = "tui-input-select-bar";
                        calbox.appendChild(todayLine);
                        pop.show(calbox, _this._button, "Rb");
                        calendar.focus();
                    } else if (_this.type() === "select") {
                        var pop = tui.ctrl.popup();
                        var list = tui.ctrl.list();
                        list.consumeMouseWheelEvent(true);
                        list.rowcheckable(false);
                        list.on("rowclick", function (data) {
                            if (self.readonly()) {
                                pop.close();
                                self.focus();
                                return false;
                            }
                            self.selectValue([list.activeItem()]);
                            pop.close();
                            self.focus();
                            if (self.fire("select", { ctrl: self[0], type: self.type(), item: list.activeItem() }) === false)
                                return;
                            self.doSubmit();
                        });
                        list.on("keydown", function (data) {
                            if (data["event"].keyCode === 13) {
                                if (self.readonly()) {
                                    pop.close();
                                    self.focus();
                                    return false;
                                }
                                self.selectValue([list.activeItem()]);
                                pop.close();
                                self.focus();
                                if (self.fire("select", { ctrl: self[0], type: self.type(), item: list.activeItem() }) === false)
                                    return;
                                self.doSubmit();
                            }
                        });
                        list[0].style.width = self[0].offsetWidth + "px";
                        list.data(self._data);
                        pop.show(list[0], self._button, "Rb");

                        var items = self._data ? self._data.length() : 0;
                        if (items < 1)
                            items = 1;
                        else if (items > 6)
                            items = 6;

                        list[0].style.height = items * list.lineHeight() + 4 + "px";
                        list.refresh();
                        pop.refresh();
                        var val = _this.selectValue();
                        if (val && val.length > 0) {
                            list.activeRowByKey(val[0].key);
                            list.scrollTo(list.activerow());
                        }
                        list.focus();
                    } else if (_this.type() === "multi-select") {
                        var pop = tui.ctrl.popup();
                        var list = tui.ctrl.list();
                        list.consumeMouseWheelEvent(true);

                        var calbox = document.createElement("div");
                        calbox.appendChild(list[0]);

                        list[0].style.width = self[0].offsetWidth + "px";
                        list.data(self._data);
                        list.uncheckAllItems();
                        var keys = getKeys(_this.selectValue());
                        list.checkItems(keys);
                        calbox.appendChild(list[0]);
                        var bar = document.createElement("div");
                        bar.className = "tui-input-select-bar";
                        calbox.appendChild(bar);
                        var okLink = document.createElement("a");
                        okLink.innerHTML = "<i class='fa fa-check'></i> " + tui.str("Accept");
                        okLink.href = "javascript:void(0)";
                        $(okLink).click(function (e) {
                            if (self.readonly()) {
                                pop.close();
                                self.focus();
                                return false;
                            }
                            self.selectValue(list.checkedItems());
                            pop.close();
                            self.focus();
                            if (self.fire("select", { ctrl: self[0], type: self.type(), checkedItems: list.checkedItems() }) === false)
                                return;
                            self.doSubmit();
                        });
                        list.on("keydown", function (data) {
                            if (data["event"].keyCode === 13) {
                                if (self.readonly()) {
                                    pop.close();
                                    self.focus();
                                    return false;
                                }
                                self.selectValue(list.checkedItems());
                                pop.close();
                                self.focus();
                                if (self.fire("select", { ctrl: self[0], type: self.type(), checkedItems: list.checkedItems() }) === false)
                                    return;
                                self.doSubmit();
                            }
                        });
                        bar.appendChild(okLink);

                        pop.show(calbox, self._button, "Rb");

                        var items = self._data ? self._data.length() : 0;
                        if (items < 1)
                            items = 1;
                        else if (items > 6)
                            items = 6;

                        list[0].style.height = items * list.lineHeight() + 4 + "px";
                        list.refresh();
                        pop.refresh();
                        list.focus();
                    } else if (_this.type() === "file") {
                        // Don't need do anything
                    } else {
                        _this.fire("btnclick", { "ctrl": _this[0], "event": e });
                    }
                };
                $(this._button).on("click", openPopup);

                $(this._label).on("mousedown", function (e) {
                    if (!_this.disabled() && (_this.type() === "text" || _this.type() === "password" || _this.type() === "custom-text"))
                        setTimeout(function () {
                            _this._textbox.focus();
                        }, 0);
                    else if (_this.type() === "select" || _this.type() === "multi-select" || _this.type() === "calendar") {
                        openPopup(e);
                    } else if (_this.type() === "file") {
                    }
                });

                $(this[0]).on("keydown", function (e) {
                    if (e.keyCode !== 32)
                        return;
                    if (_this.type() === "select" || _this.type() === "multi-select" || _this.type() === "calendar") {
                        e.preventDefault();
                        e.stopPropagation();
                    }
                });
                $(this[0]).on("keyup", function (e) {
                    if (e.keyCode !== 32)
                        return;
                    if (_this.type() === "select" || _this.type() === "multi-select" || _this.type() === "calendar") {
                        openPopup(e);
                        e.preventDefault();
                        e.stopPropagation();
                    }
                });

                if (this.type() === "select" || this.type() === "multi-select") {
                    var predefined = this.attr("data-data");
                    if (predefined)
                        predefined = eval("(" + predefined + ")");
                    if (predefined)
                        this.data(predefined);
                }
                this.value(this.value());
                //this.refresh();
            }
            Input.prototype.doSubmit = function () {
                var formId = this.submitForm();
                if (formId) {
                    var form = tui.ctrl.form(formId);
                    form && form.submit();
                }
            };

            Input.prototype.createTextbox = function () {
                var _this = this;
                var self = this;
                var type = this.type();
                if (this._textbox) {
                    this[0].removeChild(this._textbox);
                }
                this._textbox = document.createElement("input");
                if (type === "password") {
                    this._textbox.type = "password";
                } else {
                    this._textbox.type = "text";
                }

                // Should put textbox before button
                this[0].insertBefore(this._textbox, this._button);

                // Bind events ...
                $(this._textbox).on("focus", function () {
                    $(_this[0]).addClass("tui-focus");
                });
                $(this._textbox).on("blur", function () {
                    $(_this[0]).removeClass("tui-focus");
                });
                $(this._textbox).on("propertychange", function (e) {
                    if (e.originalEvent.propertyName !== 'value')
                        return;
                    setTimeout(function () {
                        if (_this.text() !== _this._textbox.value) {
                            _this.text(_this._textbox.value);
                            self.fire("change", { "ctrl": _this[0], "event": e, "text": _this.text() });
                            _this.refresh();
                        }
                    }, 0);
                });
                $(this._textbox).on("change", function (e) {
                    if (_this.text() !== _this._textbox.value) {
                        _this.text(_this._textbox.value);
                        _this.fire("change", { "ctrl": _this[0], "event": e, "text": _this.text() });
                        _this.refresh();
                    }
                });
                $(this._textbox).on("input", function (e) {
                    setTimeout(function () {
                        if (_this.text() !== _this._textbox.value) {
                            _this.text(_this._textbox.value);
                            self.fire("change", { "ctrl": self[0], "event": e, "text": self.text() });
                            _this.refresh();
                        }
                    }, 0);
                });
                $(this._textbox).keydown(function (e) {
                    if (e.keyCode === 13) {
                        _this.doSubmit();
                    }
                });
            };

            Input.prototype.makeFileUpload = function () {
                var _this = this;
                if (this._binding)
                    return;
                this._binding = tui.bindUpload(this._button, {
                    action: this.uploadUrl(),
                    name: 'file',
                    autoSubmit: true,
                    accept: this.accept(),
                    responseType: "json"
                });
                this._binding.on("change", function (data) {
                    _this.focus();
                    var result = _this.validate(data["file"]);
                    if (!result) {
                        _this.value(null);
                        _this._invalid = true;
                        _this.refresh();
                    }
                    return result;
                });
                this._binding.on("complete", function (data) {
                    data["ctrl"] = _this[0];
                    if (_this.fire("complete", data) === false)
                        return;
                    var response = data["response"];
                    if (response) {
                        response.file = data["file"];
                        _this.value(response);
                    } else {
                        tui.errbox(tui.str("Upload failed!"), tui.str("Error"));
                    }
                });
            };

            Input.prototype.unmakeFileUpload = function () {
                if (this._binding) {
                    this._binding.uninstallBind();
                    this._binding = null;
                }
            };

            Input.prototype.formatSelectText = function (val) {
                var text = "";
                for (var i = 0; i < val.length; i++) {
                    if (text.length > 0)
                        text += "; ";
                    text += validText(val[this._valueColumnKey]);
                }
                return text;
            };

            Input.prototype.formatSelectTextByData = function (val) {
                var self = this;
                var map = {};
                function buildMap(children) {
                    for (var i = 0; i < children.length; i++) {
                        if (!children[i])
                            continue;
                        var k = children[i][self._keyColumKey];
                        map[k] = children[i][self._valueColumnKey];
                        var myChildren = children[i][self._childrenColumKey];
                        if (myChildren && myChildren.length > 0) {
                            buildMap(myChildren);
                        }
                    }
                }
                var data = this._data;
                data && typeof data.src === "function" && buildMap(data.src());
                var text = "";
                for (var i = 0; i < val.length; i++) {
                    if (text.length > 0)
                        text += "; ";
                    var t = map[val[i][self._keyColumKey]];
                    if (typeof t === tui.undef)
                        t = validText(val[i][self._valueColumnKey]);
                    else
                        t = validText(t);
                    text += t;
                }
                return text;
            };

            Input.prototype.columnKey = function (key) {
                var val = this._columnKeyMap[key];
                if (typeof val === "number" && val >= 0)
                    return val;
                else
                    return key;
            };

            Input.prototype.onlyKeyValue = function (value) {
                var result = [];
                for (var i = 0; i < value.length; i++) {
                    if (typeof value[i][this._keyColumKey] !== tui.undef) {
                        var item = { key: value[i][this._keyColumKey] };
                        if (typeof value[i][this._valueColumnKey] !== tui.undef)
                            item.value = value[i][this._valueColumnKey];
                        result.push(item);
                    }
                }
                return JSON.stringify(result);
            };

            Input.prototype.fileId = function () {
                return this._fileId;
            };

            Input.prototype.type = function (txt) {
                var type;
                if (typeof txt === "string") {
                    type = this.type();
                    if (type === txt) {
                        this.refresh();
                        return this;
                    }
                    txt = txt.toLowerCase();
                    if (Input._supportType.indexOf(txt) >= 0) {
                        this.attr("data-type", txt);
                        this.createTextbox();
                        this.refresh();
                    }
                    return this;
                } else {
                    type = this.attr("data-type");
                    if (!type)
                        return "text";
                    else
                        type = type.toLowerCase();
                    if (Input._supportType.indexOf(type) >= 0) {
                        return type;
                    } else
                        return "text";
                }
            };

            Input.prototype.validator = function (val) {
                if (typeof val === "object" && val) {
                    this.attr("data-validator", JSON.stringify(val));
                    this._invalid = false;
                    this.refresh();
                    return this;
                } else if (val === null) {
                    this.removeAttr("data-validator");
                    this._invalid = false;
                    this.refresh();
                    return this;
                } else {
                    val = this.attr("data-validator");
                    if (val === null) {
                        return null;
                    } else {
                        try  {
                            val = eval("(" + val + ")");
                            if (typeof val !== "object")
                                return null;
                            else
                                return val;
                        } catch (err) {
                            return null;
                        }
                    }
                }
            };

            Input.prototype.validate = function (txt) {
                var finalText = typeof txt === "string" ? txt : this.text();
                if (finalText === null)
                    finalText = "";
                this._invalid = false;
                var validator = this.validator();
                if (validator) {
                    for (var k in validator) {
                        if (k && validator.hasOwnProperty(k)) {
                            if (k === "*password") {
                                if (!/[a-z]/.test(finalText) || !/[A-Z]/.test(finalText) || !/[0-9]/.test(finalText) || !/[\~\`\!\@\#\$\%\^\&\*\(\)\_\-\+\=\\\]\[\{\}\:\;\"\'\/\?\,\.\<\>\|]/.test(finalText) || finalText.length < 6) {
                                    this._invalid = true;
                                }
                            } else if (k.substr(0, 8) === "*maxlen:") {
                                var imaxLen = parseFloat(k.substr(8));
                                if (isNaN(imaxLen))
                                    throw new Error("Invalid validator: '*maxlen:...' must follow a number");
                                var ival = finalText.length;
                                if (ival > imaxLen) {
                                    this._invalid = true;
                                }
                            } else if (k.substr(0, 8) === "*minlen:") {
                                var iminLen = parseFloat(k.substr(8));
                                if (isNaN(iminLen))
                                    throw new Error("Invalid validator: '*iminLen:...' must follow a number");
                                var ival = finalText.length;
                                if (ival < iminLen) {
                                    this._invalid = true;
                                }
                            } else if (k.substr(0, 5) === "*max:") {
                                var imax = parseFloat(k.substr(5));
                                if (isNaN(imax))
                                    throw new Error("Invalid validator: '*max:...' must follow a number");
                                var ival = parseFloat(finalText);
                                if (isNaN(ival) || ival > imax) {
                                    this._invalid = true;
                                }
                            } else if (k.substr(0, 5) === "*min:") {
                                var imin = parseFloat(k.substr(5));
                                if (isNaN(imin))
                                    throw new Error("Invalid validator: '*min:...' must follow a number");
                                var ival = parseFloat(finalText);
                                if (isNaN(ival) || ival < imin) {
                                    this._invalid = true;
                                }
                            } else if (k.substr(0, 6) === "*same:") {
                                var other = k.substr(6);
                                other = input(other);
                                if (other) {
                                    var otherText = other.text();
                                    if (otherText === null)
                                        otherText = "";
                                    if (finalText !== otherText)
                                        this._invalid = true;
                                } else {
                                    this._invalid = true;
                                }
                            } else {
                                var regexp;
                                if (k.substr(0, 1) === "*") {
                                    var v = Input.VALIDATORS[k];
                                    if (v)
                                        regexp = new RegExp(v);
                                    else
                                        throw new Error("Invalid validator: " + k + " is not a valid validator");
                                } else {
                                    regexp = new RegExp(k);
                                }
                                this._invalid = !regexp.test(finalText);
                            }
                            if (this._invalid) {
                                this._message = validator[k];
                                break;
                            }
                        }
                    }
                }
                if (this._invalid && !this._message) {
                    this._message = tui.str("Invalid input.");
                }
                this.refresh();
                return !this._invalid;
            };

            Input.prototype.uploadUrl = function (url) {
                if (typeof url === "string") {
                    this.attr("data-upload-url", url);
                    this.refresh();
                    return this;
                } else
                    return this.attr("data-upload-url");
            };

            Input.prototype.text = function (txt) {
                var type = this.type();
                if (typeof txt === "string") {
                    if (type === "text" || type === "password" || type === "custom-text") {
                        this.attr("data-text", txt);
                        this.attr("data-value", txt);
                        this._invalid = false;
                        this.refresh();
                    }
                    return this;
                } else
                    return this.attr("data-text");
            };

            Input.prototype.accept = function (txt) {
                var type = this.type();
                if (typeof txt === "string") {
                    this.attr("data-accept", txt);
                    this.refresh();
                    return this;
                } else
                    return this.attr("data-accept");
            };

            Input.prototype.data = function (data) {
                if (data) {
                    var self = this;
                    if (data instanceof Array || data.data && data.data instanceof Array) {
                        data = new tui.ArrayProvider(data);
                    }
                    if (typeof data.length !== "function" || typeof data.sort !== "function" || typeof data.at !== "function" || typeof data.columnKeyMap !== "function") {
                        throw new Error("TUI Input: need a data provider.");
                    }
                    this._data = data;
                    if (data)
                        this._columnKeyMap = data.columnKeyMap();
                    else
                        this._columnKeyMap = {};
                    this._keyColumKey = this.columnKey("key");
                    this._valueColumnKey = this.columnKey("value");
                    this._childrenColumKey = this.columnKey("children");
                    return this;
                } else
                    return this._data;
            };

            Input.prototype.valueHasText = function (val) {
                if (typeof val === "boolean") {
                    this.is("data-value-has-text", val);
                    return this;
                } else
                    return this.is("data-value-has-text");
            };

            Input.prototype.valueToSelect = function (val) {
                if (this.valueHasText()) {
                    return val;
                } else {
                    if (this.type() === "select") {
                        val = [val];
                    }
                    var newval = [];
                    if (val && val.length > 0) {
                        for (var i = 0; i < val.length; i++) {
                            newval.push({ key: val[i] });
                        }
                    }
                    return newval;
                }
            };

            Input.prototype.selectToValue = function (val) {
                if (this.valueHasText()) {
                    return val;
                } else {
                    if (this.type() === "select") {
                        if (val && val.length > 0)
                            return val[0].key;
                        else
                            return null;
                    } else {
                        var newval = [];
                        if (val && val.length > 0) {
                            for (var i = 0; i < val.length; i++) {
                                newval.push(val[i].key);
                            }
                        }
                        return newval;
                    }
                }
            };

            Input.prototype.selectValue = function (val) {
                var type = this.type();
                if (typeof val !== tui.undef) {
                    if (type === "select" || type === "multi-select") {
                        if (val && typeof val.length === "number") {
                            this.attr("data-value", this.onlyKeyValue(val));
                            this.attr("data-text", this.formatSelectTextByData(val));
                            this._invalid = false;
                        } else if (val === null) {
                            this.attr("data-value", "[]");
                            this.attr("data-text", "");
                            this._invalid = false;
                        }
                        this.refresh();
                    }
                    return this;
                } else {
                    val = this.attr("data-value");
                    if (type === "select" || type === "multi-select") {
                        if (val === null) {
                            return [];
                        }
                        return eval("(" + val + ")");
                    } else
                        return null;
                }
            };

            Input.prototype.readonly = function (val) {
                if (typeof val === "boolean") {
                    this.is("data-readonly", val);
                    this.refresh();
                    return this;
                } else
                    return this.is("data-readonly");
            };

            Input.prototype.value = function (val) {
                var type = this.type();
                if (typeof val !== tui.undef) {
                    if (val == null) {
                        this.removeAttr("data-value");
                        this.attr("data-text", "");
                        this._invalid = false;
                        this.refresh();
                    } else if (type === "calendar") {
                        if (typeof val === "string") {
                            try  {
                                val = tui.parseDate(val);
                            } catch (e) {
                                val = null;
                            }
                        }
                        if (val instanceof Date) {
                            this.attr("data-value", tui.formatDate(val, "yyyy-MM-dd"));
                            this.attr("data-text", tui.formatDate(val, tui.str("yyyy-MM-dd")));
                            this._invalid = false;
                        }
                        this.refresh();
                    } else if (type === "file") {
                        if (val === null) {
                            this.attr("data-value", JSON.stringify(val));
                            this.attr("data-text", "");
                            this._invalid = false;
                            this.refresh();
                        } else if (val.file && val.fileId) {
                            this.attr("data-value", JSON.stringify(val));
                            this.attr("data-text", val.file);
                            this._invalid = false;
                            this.refresh();
                        }
                    } else if (type === "text" || type === "password" || type === "custom-text") {
                        this.attr("data-text", val);
                        this.attr("data-value", val);
                        this._invalid = false;
                        this.refresh();
                    } else if (type === "select" || type === "multi-select") {
                        this.selectValue(this.valueToSelect(val));
                    }
                    return this;
                } else {
                    val = this.attr("data-value");
                    if (type === "calendar") {
                        if (val === null)
                            return null;
                        return val;
                    } else if (type === "file") {
                        if (val === null)
                            return null;
                        return eval("(" + val + ")");
                    } else if (type === "select" || type === "multi-select") {
                        return this.selectToValue(this.selectValue());
                    } else
                        return val;
                }
            };

            Input.prototype.icon = function (txt) {
                if (typeof txt === "string") {
                    this.attr("data-icon", txt);
                    this.refresh();
                    return this;
                } else
                    return this.attr("data-icon");
            };

            Input.prototype.placeholder = function (txt) {
                if (typeof txt === "string") {
                    this.attr("data-placeholder", txt);
                    this.refresh();
                    return this;
                } else
                    return this.attr("data-placeholder");
            };

            Input.prototype.submitForm = function (formId) {
                if (typeof formId === "string") {
                    this.attr("data-submit-form", formId);
                    return this;
                } else
                    return this.attr("data-submit-form");
            };

            Input.prototype.refresh = function () {
                var type = this.type().toLowerCase();
                if (type === "file" && !this.readonly()) {
                    this.makeFileUpload();
                } else
                    this.unmakeFileUpload();
                var placeholder = this.placeholder();
                if (placeholder === null)
                    placeholder = "";
                var text = this.text();
                if (text === null)
                    text = "";
                var withBtn = false;
                if (type !== "text" && type !== "password") {
                    withBtn = true;
                    this._button.style.height = "";
                    this._button.style.height = ($(this[0]).innerHeight() - ($(this._button).outerHeight() - $(this._button).height())) + "px";
                    this._button.style.lineHeight = this._button.style.height;
                    this._button.style.display = "";
                } else {
                    this._button.style.display = "none";
                }
                if (this.icon()) {
                    $(this._button).addClass(this.icon());
                } else
                    this._button.className = "";
                if (type === "text" || type === "password" || type === "custom-text") {
                    if (this.readonly())
                        this._textbox.readOnly = true;
                    else
                        this._textbox.readOnly = false;
                    if (this._textbox.value !== text)
                        this._textbox.value = text;
                    this.removeAttr("tabIndex");
                    this._textbox.style.display = "";
                    this._label.style.display = "none";
                    if (withBtn) {
                        this._button.style.display = "";
                        this._textbox.style.width = "";
                        this._textbox.style.width = ($(this[0]).innerWidth() - ($(this._textbox).outerWidth() - $(this._textbox).width()) - $(this._button).outerWidth()) + "px";
                    } else {
                        this._button.style.display = "none";
                        this._textbox.style.width = "";
                        this._textbox.style.width = ($(this[0]).innerWidth() - ($(this._textbox).outerWidth() - $(this._textbox).width())) + "px";
                    }
                    this._textbox.style.height = "";
                    this._textbox.style.height = ($(this[0]).innerHeight() - ($(this._textbox).outerHeight() - $(this._textbox).height())) + "px";
                    this._textbox.style.lineHeight = this._textbox.style.height;
                    this._label.style.width = this._textbox.style.width;
                } else {
                    this._label.innerHTML = text;
                    this._textbox.style.display = "none";
                    this._label.style.display = "";
                    this._label.style.right = "";
                    this.attr("tabIndex", "0");
                    this._label.style.lineHeight = $(this._label).height() + "px";
                }
                if (placeholder && !text) {
                    this._label.innerHTML = placeholder;
                    this._label.style.display = "";
                    $(this._label).addClass("tui-placeholder");
                    this._label.style.lineHeight = $(this._label).height() + "px";
                } else {
                    $(this._label).removeClass("tui-placeholder");
                }
                if (this._invalid) {
                    //if (tui.ieVer > 0 && tui.ieVer < 9)
                    //	$(this._notify).attr("title", this._message);
                    //else
                    //	$(this._notify).attr("data-warning", this._message);
                    $(this._notify).attr("data-tooltip", this._message);
                    $(this._notify).css({
                        "display": "",
                        "right": (withBtn ? this._button.offsetWidth : 0) + "px"
                    });
                    $(this._notify).css({
                        "line-height": this._notify.offsetHeight + "px"
                    });
                } else {
                    $(this._notify).css("display", "none");
                }
            };
            Input.CLASS = "tui-input";
            Input.VALIDATORS = {
                "*email": "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$",
                "*chinese": "^[\\u4e00-\\u9fa5]+$",
                "*url": "^http://([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?$",
                "*digital": "^\\d+$",
                "*integer": "^[+\\-]?\\d+$",
                "*float": "^[+\\-]?\\d*\\.\\d+$",
                "*currency": "^-?\\d{1,3}(,\\d{3})*\\.\\d{2,3}$",
                "*date": "^[0-9]{4}-1[0-2]|0?[1-9]-0?[1-9]|[12][0-9]|3[01]$",
                "*any": "\\S+"
            };

            Input._supportType = [
                "text", "password", "select", "multi-select",
                "calendar", "file", "custom-select", "custom-text"];
            return Input;
        })(_ctrl.Control);
        _ctrl.Input = Input;

        /**
        * Construct a button.
        * @param el {HTMLElement or element id or construct info}
        */
        function input(param, type) {
            return tui.ctrl.control(param, Input, type);
        }
        _ctrl.input = input;

        tui.ctrl.registerInitCallback(Input.CLASS, input);
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;
})(tui || (tui = {}));
var tui;
(function (tui) {
    /// <reference path="tui.ctrl.input.ts" />
    (function (_ctrl) {
        var TextArea = (function (_super) {
            __extends(TextArea, _super);
            function TextArea(el) {
                var _this = this;
                _super.call(this, "div", TextArea.CLASS, el);
                this._invalid = false;
                this._message = "";
                var self = this;

                this._label = document.createElement("label");
                this._notify = document.createElement("div");
                this[0].innerHTML = "";
                this[0].appendChild(this._label);
                this[0].appendChild(this._notify);
                this.createTextbox();

                $(this._label).on("mousedown", function () {
                    if (!_this.disabled())
                        setTimeout(function () {
                            _this._textbox.focus();
                        }, 0);
                });
                this.value(this.value());
                //this.refresh();
            }
            TextArea.prototype.createTextbox = function () {
                var _this = this;
                var self = this;
                if (this._textbox) {
                    this[0].removeChild(this._textbox);
                }
                this._textbox = document.createElement("textarea");

                // Should put textbox before notify
                this[0].insertBefore(this._textbox, this._notify);

                // Bind events ...
                $(this._textbox).on("focus", function () {
                    $(_this[0]).addClass("tui-focus");
                });
                $(this._textbox).on("blur", function () {
                    $(_this[0]).removeClass("tui-focus");
                });
                $(this._textbox).on("propertychange", function (e) {
                    if (e.originalEvent.propertyName !== 'value')
                        return;
                    setTimeout(function () {
                        if (_this.text() !== _this._textbox.value) {
                            _this.text(_this._textbox.value);
                            self.fire("change", { "ctrl": _this[0], "event": e, "text": _this.text() });
                            _this.refresh();
                        }
                    }, 0);
                });
                $(this._textbox).on("change", function (e) {
                    if (_this.text() !== _this._textbox.value) {
                        _this.text(_this._textbox.value);
                        _this.fire("change", { "ctrl": _this[0], "event": e, "text": _this.text() });
                        _this.refresh();
                    }
                });
                $(this._textbox).on("input", function (e) {
                    setTimeout(function () {
                        if (_this.text() !== _this._textbox.value) {
                            _this.text(_this._textbox.value);
                            self.fire("change", { "ctrl": self[0], "event": e, "text": self.text() });
                            _this.refresh();
                        }
                    }, 0);
                });
            };

            TextArea.prototype.validator = function (val) {
                if (typeof val === "object" && val) {
                    this.attr("data-validator", JSON.stringify(val));
                    this._invalid = false;
                    this.refresh();
                    return this;
                } else if (val === null) {
                    this.removeAttr("data-validator");
                    this._invalid = false;
                    this.refresh();
                    return this;
                } else {
                    val = this.attr("data-validator");
                    if (val === null) {
                        return null;
                    } else {
                        try  {
                            val = eval("(" + val + ")");
                            if (typeof val !== "object")
                                return null;
                            else
                                return val;
                        } catch (err) {
                            return null;
                        }
                    }
                }
            };

            TextArea.prototype.validate = function (txt) {
                var finalText = typeof txt === "string" ? txt : this.text();
                if (finalText === null)
                    finalText = "";
                this._invalid = false;
                var validator = this.validator();
                if (validator) {
                    for (var k in validator) {
                        if (k && validator.hasOwnProperty(k)) {
                            if (k === "*password") {
                                if (!/[a-z]/.test(finalText) || !/[A-Z]/.test(finalText) || !/[0-9]/.test(finalText) || !/[\~\`\!\@\#\$\%\^\&\*\(\)\_\-\+\=\\\]\[\{\}\:\;\"\'\/\?\,\.\<\>\|]/.test(finalText) || finalText.length < 6) {
                                    this._invalid = true;
                                }
                            } else if (k.substr(0, 5) === "*max:") {
                                var imax = parseFloat(k.substr(5));
                                if (isNaN(imax))
                                    throw new Error("Invalid validator: '*max:...' must follow a number");
                                var ival = parseFloat(finalText);
                                if (isNaN(ival) || ival > imax) {
                                    this._invalid = true;
                                }
                            } else if (k.substr(0, 4) === "*min:") {
                                var imin = parseFloat(k.substr(5));
                                if (isNaN(imin))
                                    throw new Error("Invalid validator: '*min:...' must follow a number");
                                var ival = parseFloat(finalText);
                                if (isNaN(ival) || ival < imin) {
                                    this._invalid = true;
                                }
                            } else {
                                var regexp;
                                if (k.substr(0, 1) === "*") {
                                    var v = _ctrl.Input.VALIDATORS[k];
                                    if (v)
                                        regexp = new RegExp(v);
                                    else
                                        throw new Error("Invalid validator: " + k + " is not a valid validator");
                                } else {
                                    regexp = new RegExp(k);
                                }
                                this._invalid = !regexp.test(finalText);
                            }
                            if (this._invalid) {
                                this._message = validator[k];
                                break;
                            }
                        }
                    }
                }
                if (this._invalid && !this._message) {
                    this._message = tui.str("Invalid input.");
                }
                this.refresh();
                return !this._invalid;
            };

            TextArea.prototype.text = function (txt) {
                if (typeof txt !== tui.undef) {
                    this.attr("data-text", txt);
                    this.attr("data-value", txt);
                    this._invalid = false;
                    this.refresh();
                    return this;
                } else
                    return this.attr("data-text");
            };

            TextArea.prototype.readonly = function (val) {
                if (typeof val === "boolean") {
                    this.is("data-readonly", val);
                    this.refresh();
                    return this;
                } else
                    return this.is("data-readonly");
            };

            TextArea.prototype.value = function (val) {
                if (typeof val !== tui.undef) {
                    if (val === null) {
                        this.attr("data-text", "");
                        this.attr("data-value", "");
                    } else {
                        this.attr("data-text", val);
                        this.attr("data-value", val);
                    }
                    this._invalid = false;
                    this.refresh();
                    return this;
                } else {
                    val = this.attr("data-value");
                    return val;
                }
            };

            TextArea.prototype.autoResize = function (val) {
                if (typeof val === "boolean") {
                    this.is("data-auto-resize", val);
                    this.refresh();
                    return this;
                } else
                    return this.is("data-auto-resize");
            };

            TextArea.prototype.placeholder = function (txt) {
                if (typeof txt === "string") {
                    this.attr("data-placeholder", txt);
                    this.refresh();
                    return this;
                } else
                    return this.attr("data-placeholder");
            };
            TextArea.prototype.refresh = function () {
                var placeholder = this.placeholder();
                if (placeholder === null)
                    placeholder = "";
                var text = this.text();
                if (text === null)
                    text = "";
                var withBtn = false;

                if (this._textbox.value !== text)
                    this._textbox.value = text;
                if (this.readonly())
                    this._textbox.readOnly = true;
                else
                    this._textbox.readOnly = false;
                this._textbox.style.display = "";
                this._label.style.display = "none";
                this._textbox.style.width = "";
                this._textbox.style.width = ($(this[0]).innerWidth() - ($(this._textbox).outerWidth() - $(this._textbox).width())) + "px";

                //this._textbox.scrollHeight
                this._textbox.style.height = "";
                var maxHeight = parseInt($(this[0]).css("max-height"), 10);
                if (this._textbox.scrollHeight < maxHeight || isNaN(maxHeight)) {
                    this._textbox.style.overflow = "hidden";
                    $(this[0]).css("height", this._textbox.scrollHeight + "px");
                } else {
                    this._textbox.style.overflow = "auto";
                    $(this[0]).css("height", maxHeight + "px");
                }

                this._textbox.style.height = ($(this[0]).innerHeight() - ($(this._textbox).outerHeight() - $(this._textbox).height())) + "px";

                //this._textbox.style.lineHeight = this._textbox.style.height;
                this._label.style.width = this._textbox.style.width;

                if (placeholder && !text) {
                    this._label.innerHTML = placeholder;
                    this._label.style.display = "";
                    $(this._label).addClass("tui-placeholder");
                    this._label.style.lineHeight = $(this._label).height() + "px";
                } else {
                    $(this._label).removeClass("tui-placeholder");
                }
                if (this._invalid) {
                    $(this._notify).attr("data-tooltip", this._message);
                    $(this._notify).css({
                        "display": "",
                        "right": "0px"
                    });
                    $(this._notify).css({
                        "line-height": this._notify.offsetHeight + "px"
                    });
                } else {
                    $(this._notify).css("display", "none");
                }
            };
            TextArea.CLASS = "tui-textarea";
            return TextArea;
        })(_ctrl.Control);
        _ctrl.TextArea = TextArea;

        /**
        * Construct a button.
        * @param el {HTMLElement or element id or construct info}
        */
        function textarea(param) {
            return tui.ctrl.control(param, TextArea);
        }
        _ctrl.textarea = textarea;

        tui.ctrl.registerInitCallback(TextArea.CLASS, textarea);
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;
})(tui || (tui = {}));
var tui;
(function (tui) {
    /// <reference path="tui.ctrl.form.ts" />
    (function (ctrl) {
        var Tab = (function (_super) {
            __extends(Tab, _super);
            function Tab(el) {
                _super.call(this, "div", Tab.CLASS, el);
                this._tabId = "tab-" + tui.uuid();
                this._buttons = [];

                var removeList = [];
                var activeIndex = 0;
                for (var i = 0; i < this[0].childNodes.length; i++) {
                    var child = this[0].childNodes[i];
                    if (child.nodeName.toLowerCase() === "span" || child.nodeName.toLowerCase() === "a") {
                        $(child).addClass("tui-radiobox");
                        var button = tui.ctrl.radiobox(child);
                        button.group(this._tabId);
                        this._buttons.push(button);
                        if (button.checked())
                            activeIndex = this._buttons.length - 1;
                        button.on("check", this.checkPage);
                    } else
                        removeList.push(child);
                }
                for (var i = 0; i < removeList.length; i++) {
                    tui.removeNode(removeList[i]);
                }
                this.at(activeIndex).checked(true);
            }
            Tab.prototype.checkPage = function (data) {
                var tabId = data.ctrl.attr("data-tab");
                tabId = "#" + tabId;
                if (data.ctrl.checked()) {
                    $(tabId).removeClass("tui-hidden");
                } else {
                    $(tabId).addClass("tui-hidden");
                }
            };

            Tab.prototype.at = function (index) {
                if (index >= 0 && index < this._buttons.length)
                    return this._buttons[index];
                else
                    return null;
            };

            Tab.prototype.add = function (name, index) {
                var button = tui.ctrl.radiobox();
                button.text(name);
                button.on("check", this.checkPage);
                if (typeof index === tui.undef) {
                    this[0].appendChild(button[0]);
                    this._buttons.push(button);
                } else {
                    this[0].insertBefore(button[0], this.at(index)[0]);
                    this._buttons.splice(index, 0, button);
                }
                return this;
            };

            Tab.prototype.remove = function (index) {
                var button = this.at(index);
                if (button) {
                    button.off("check", this.checkPage);
                    this._buttons.splice(index, 1);
                    tui.removeNode(button[0]);
                }
                return this;
            };

            Tab.prototype.active = function (index) {
                if (typeof index !== tui.undef) {
                    var button = this.at(index);
                    if (button) {
                        button.checked(true);
                    }
                    return this;
                } else {
                    for (var i = 0; i < this._buttons.length; i++) {
                        if (this._buttons[i].checked())
                            return i;
                    }
                    return -1;
                }
            };
            Tab.CLASS = "tui-tab";
            return Tab;
        })(ctrl.Control);
        ctrl.Tab = Tab;

        function tab(param) {
            return tui.ctrl.control(param, Tab);
        }
        ctrl.tab = tab;
        tui.ctrl.registerInitCallback(Tab.CLASS, tab);
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;
})(tui || (tui = {}));
var tui;
(function (tui) {
    /// <reference path="tui.ctrl.control.ts" />
    (function (_ctrl) {
        var Paginator = (function (_super) {
            __extends(Paginator, _super);
            function Paginator(el) {
                _super.call(this, "div", Paginator.CLASS, el);

                if (!this.hasAttr("data-max-buttons"))
                    this.maxButtons(3);
                if (!this.hasAttr("data-value"))
                    this.value(1);
                if (!this.hasAttr("data-page-size"))
                    this.pageSize(10);
                if (!this.hasAttr("data-total-size"))
                    this.totalSize(0);

                this.refresh();
            }
            Paginator.prototype.value = function (val) {
                if (typeof val !== tui.undef) {
                    if (typeof val === "number") {
                        if (val > this.totalPages())
                            val = this.totalPages();
                        if (val < 1)
                            val = 1;
                        this.attr("data-value", val);
                        this.refresh();
                    }
                    return this;
                } else
                    return Math.round(parseInt(this.attr("data-value")));
            };

            Paginator.prototype.totalPages = function () {
                var total = Math.ceil(this.totalSize() / this.pageSize());
                if (total < 1)
                    total = 1;
                return total;
            };

            Paginator.prototype.pageSize = function (val) {
                if (typeof val !== tui.undef) {
                    if (typeof val === "number") {
                        if (val <= 0)
                            val = 1;
                        this.attr("data-page-size", val);
                        this.refresh();
                    }
                    return this;
                } else
                    return Math.round(parseInt(this.attr("data-page-size")));
            };

            Paginator.prototype.totalSize = function (val) {
                if (typeof val !== tui.undef) {
                    if (typeof val === "number") {
                        this.attr("data-total-size", val);
                        this.refresh();
                    }
                    return this;
                } else
                    return Math.round(parseInt(this.attr("data-total-size")));
            };

            Paginator.prototype.submitForm = function (formId) {
                if (typeof formId === "string") {
                    this.attr("data-submit-form", formId);
                    return this;
                } else
                    return this.attr("data-submit-form");
            };

            Paginator.prototype.maxButtons = function (val) {
                if (typeof val !== tui.undef) {
                    if (typeof val === "number") {
                        if (val <= 0)
                            val = 1;
                        this.attr("data-max-buttons", val);
                        this.refresh();
                    }
                    return this;
                } else
                    return Math.round(parseInt(this.attr("data-max-buttons")));
            };

            Paginator.prototype.changeValue = function (val) {
                this.value(val);

                //this.refresh();
                if (this.fire("change", { ctrl: this[0], value: this.value() }) === false)
                    return;
                var formId = this.submitForm();
                if (formId) {
                    var form = tui.ctrl.form(formId);
                    form && form.submit();
                }
            };

            Paginator.prototype.refresh = function () {
                if (!this[0])
                    return;
                var self = this;
                this[0].innerHTML = "";

                // Add Previous Button
                var previous = _ctrl.button();
                previous.text(tui.str("Previous"));
                this[0].appendChild(previous[0]);
                if (this.value() === 1) {
                    previous.disabled(true);
                } else {
                    previous.on("click", function () {
                        self.changeValue(self.value() - 1);
                    });
                }
                var maxButtons = this.maxButtons();
                var totalPages = this.totalPages();

                var fromIndex = this.value() - Math.floor(maxButtons / 2) + (maxButtons % 2 === 0 ? 1 : 0);
                if (fromIndex <= 1) {
                    fromIndex = 1;
                }
                var toIndex = (fromIndex === 1 ? fromIndex + maxButtons : fromIndex + maxButtons - 1);
                if (toIndex >= totalPages) {
                    toIndex = totalPages;
                    fromIndex = toIndex - maxButtons;
                    if (fromIndex < 1) {
                        fromIndex = 1;
                    }
                }

                if (fromIndex > 1) {
                    var btn = _ctrl.button();
                    btn.html(1 + (fromIndex > 2 ? " <i class='fa fa-ellipsis-h'></i>" : ""));
                    this[0].appendChild(btn[0]);
                    btn.on("click", function () {
                        self.changeValue(1);
                    });
                }
                for (var i = fromIndex; i <= toIndex; i++) {
                    var btn = _ctrl.button();
                    btn.text(i + "");
                    btn.on("click", function () {
                        self.changeValue(parseInt(this.text()));
                    });
                    this[0].appendChild(btn[0]);
                    if (i === this.value())
                        btn.addClass("tui-primary");
                }
                if (toIndex < totalPages) {
                    var btn = _ctrl.button();
                    btn.html((toIndex < totalPages - 1 ? "<i class='fa fa-ellipsis-h'></i> " : "") + totalPages);
                    this[0].appendChild(btn[0]);
                    btn.on("click", function () {
                        self.changeValue(totalPages);
                    });
                }

                // Add Next Button
                var next = _ctrl.button();
                next.text(tui.str("Next"));
                this[0].appendChild(next[0]);
                if (this.value() === this.totalPages()) {
                    next.disabled(true);
                } else {
                    next.on("click", function () {
                        self.changeValue(self.value() + 1);
                    });
                }
            };
            Paginator.CLASS = "tui-paginator";
            return Paginator;
        })(_ctrl.Control);
        _ctrl.Paginator = Paginator;

        function paginator(param) {
            return tui.ctrl.control(param, Paginator);
        }
        _ctrl.paginator = paginator;
        tui.ctrl.registerInitCallback(Paginator.CLASS, paginator);
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;
})(tui || (tui = {}));
var tui;
(function (tui) {
    /// <reference path="tui.ctrl.control.ts" />
    (function (_ctrl) {
        var Tips = (function (_super) {
            __extends(Tips, _super);
            function Tips(el) {
                var _this = this;
                _super.call(this, "div", Tips.CLASS, el);
                this._closeButton = null;
                var btn = document.createElement("span");
                this._closeButton = btn;
                btn.className = "tui-tips-close";
                this[0].appendChild(btn);
                $(btn).click(function (e) {
                    _this.close();
                });
            }
            Tips.prototype.useVisible = function (val) {
                if (typeof val !== tui.undef) {
                    this.is("data-use-visible", !!val);
                    return this;
                } else
                    return this.is("data-use-visible");
            };

            Tips.prototype.show = function (msg) {
                if (typeof msg !== tui.undef) {
                    tui.removeNode(this._closeButton);
                    this[0].innerHTML = msg;
                    this[0].appendChild(this._closeButton);
                }
                this.removeClass("tui-invisible");
                this.removeClass("tui-hidden");
            };

            Tips.prototype.close = function () {
                this.fire("close", { ctrl: this[0] });
                if (this.useVisible())
                    this.addClass("tui-invisible");
                else
                    this.addClass("tui-hidden");
            };
            Tips.CLASS = "tui-tips";
            return Tips;
        })(_ctrl.Control);
        _ctrl.Tips = Tips;

        /**
        * Construct a tips.
        * @param el {HTMLElement or element id or construct info}
        */
        function tips(param) {
            return tui.ctrl.control(param, Tips);
        }
        _ctrl.tips = tips;

        tui.ctrl.registerInitCallback(Tips.CLASS, tips);
    })(tui.ctrl || (tui.ctrl = {}));
    var ctrl = tui.ctrl;
})(tui || (tui = {}));
