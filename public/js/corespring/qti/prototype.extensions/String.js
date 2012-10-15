(function(){
    if(typeof(String.prototype.contains) == "undefined"){

        String.prototype.contains = function(v){
            if(!v){
                return false;
            }
            return this.indexOf(v) != -1;
        }
    }

})();