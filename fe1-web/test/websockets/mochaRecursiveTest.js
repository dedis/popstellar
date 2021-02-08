/* eslint-disable */

const assert = require('assert');

describe('Basic arithmetic:', function() {
  describe('1 + 1 = 2', function () {
    it('should be true that "1 + 1 = 2"', function () {
      assert.equal(1 + 1, 2);
    });
  });
  describe('1 + 1 + 1 = 3', function () {
    it('should be true that "1 + 1 + 1 = 3"', function () {
      assert.equal(1 + 1 + 1, 3);
    });
  });
});
