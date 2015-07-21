component extends="org.lucee.cfml.test.LuceeTestCase"   {

	function run( testResults , testBox ) {

		describe( 'LDEV-434 DeserializeJSON' , function() {

			it( 'should error when too a trailing comma is included on a simple array' , function() {
				var bad_json = '[1,]';
				expect(function(){
					DeserializeJSON( bad_json );
				}).toThrow();
			});

			it( 'should be ok if the string is ok' , function() {
				var good_json = '[1,2]';
				expect( DeserializeJSON( good_json ) ).toBe([1,2]);
			});

			it( 'should be still allow a trailing comma for a CF array' , function() {
				expect( [1,2,] ).toBe( [1,2] );
			});

		});

	}
	
} 