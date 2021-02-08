/* eslint-disable */

const assert = require('assert');
const assertChai = require('chai').assert;

describe('Array:', function() {
  describe('#indexOf()', function() {
    it('should return -1 when the value is not present', function() {
      assert.equal([1, 2, 3].indexOf(4), -1);
    });
  });
});

describe('Mocha + Chai test:', function() {
  it('should work', function() {
    assertChai.strictEqual(true, true, 'these booleans are strictly equal');
    assertChai.deepEqual({ tea: 'green' }, { tea: 'green' });
    assertChai.isAbove(5, 2, '5 is strictly greater than 2');
  });
});
