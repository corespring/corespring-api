describe('prototype.extensions.Array', function(){

    describe('shuffle', function(){

        it('should shuffle', function() {

            expect(true).toBe(true);

            var iterations = 10000;
            var array =       [0,1,2,3,4,5,6,7,8,9];
            var randomCount = [0,0,0,0,0,0,0,0,0,0];
            var i = 0;
            do{
               array.shuffle();
               randomCount[array[0]]++;
               i++;
            } while( i < iterations );

            var exactDivide = iterations / array.length;

            var allowedVariance = exactDivide * 0.15;

            for( var z = 0; z < randomCount.length ; z++ ){
                expect( randomCount[z]).toBeGreaterThan( exactDivide - allowedVariance );
                expect( randomCount[z]).toBeLessThan( exactDivide + allowedVariance );
            }
        });
    });
});