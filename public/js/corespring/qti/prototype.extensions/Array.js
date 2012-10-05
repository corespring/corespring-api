
if (typeof(Array.shuffle) == "undefined") {

    /**
     * Shuffle array contents
     * @param fixedIndexes - optional - an array of indexes not to shuffle
     * @return {*}
     */
    Array.prototype.shuffle = function (fixedIndexes) {

        if (!fixedIndexes) {
            fixedIndexes = []
        }

        /**
         * Randomize array element order in-place.
         * Using Fisher-Yates shuffle algorithm.
         */
        function shuffleArray(array, fixedIndexes) {

            function getRandomIndex(currentIndex) {

                var index = Math.floor(Math.random() * (currentIndex + 1));

                if (fixedIndexes.indexOf(index) == -1) {
                    return index;
                }
                else {
                    return getRandomIndex(currentIndex);
                }
            }

            for (var i = array.length - 1; i > 0; i--) {

                if (fixedIndexes.indexOf(i) == -1) {
                    var j = getRandomIndex(i);
                    var temp = array[i];
                    array[i] = array[j];
                    array[j] = temp;
                }
            }
            return array;
        }

        if (this.length <= 1) {
            return;
        }

        return shuffleArray(this, fixedIndexes);
    };

}

