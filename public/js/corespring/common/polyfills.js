if (Array.prototype.removeItem == null) {
  Array.prototype.removeItem = function (item) {
    var itemIndex = this.indexOf(item);
    if (itemIndex == -1) {
      return null;
    }
    return this.splice(itemIndex, 1)[0];
  };
}
