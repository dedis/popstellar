import { Base64UrlData } from '../Base64UrlData';

test('Base64Url should encode and decode properly', () => {
  const string: string = 'string';
  expect(Base64UrlData.encode(string).decode()).toBe(string);
});

test('Base64Url should transfer to and from buffer', () => {
  const string: string = 'string';
  const b64data = Base64UrlData.encode(string);

  expect(Base64UrlData.fromBuffer(b64data.toBuffer())).toEqual(b64data);
});
