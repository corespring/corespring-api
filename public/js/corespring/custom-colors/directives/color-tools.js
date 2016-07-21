angular.module('customColors.directives').factory("ColorTools", function() {

  function rgbToHex(r, g, b) {
    var bin = r << 16 | g << 8 | b;
    return (function(h) {
      return new Array(7-h.length).join("0") + h
    })(bin.toString(16).toUpperCase());
  }

  function hexToRgb(hex) {
    var bigint = parseInt(hex.replace(/^#/, ''), 16);
    var r = (bigint >> 16) & 255;
    var g = (bigint >> 8) & 255;
    var b = bigint & 255;
    return [r,g,b];
  }

  function rgbaToRgb(r, g, b, a) {
    var r2 = Math.round(((1 - a) * 255) + (a * r))
    var g2 = Math.round(((1 - a) * 255) + (a * g))
    var b2 = Math.round(((1 - a) * 255) + (a * b))
    return [r2, g2, b2];
  }

  function lighten(hex, a) {
    var rgb = hexToRgb(hex);
    var lightRgb = rgbaToRgb(rgb[0], rgb[1], rgb[2], a);
    return rgbToHex(lightRgb[0], lightRgb[1], lightRgb[2]);
  }

  return {
    lighten: lighten
  };

});