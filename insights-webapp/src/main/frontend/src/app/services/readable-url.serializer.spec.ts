import { ReadableUrlSerializer } from './readable-url.serializer';

describe('ReadableUrlSerializer', () => {
  let serializer: ReadableUrlSerializer;

  const roundTrip = (url: string): string => serializer.serialize(serializer.parse(url));

  beforeEach(() => {
    serializer = new ReadableUrlSerializer();
  });

  it('should leave square brackets in a query parameter unencoded', () => {
    expect(roundTrip('/graph?range=[9.0],[9.4],[10.0,)')).toBe('/graph?range=[9.0],[9.4],[10.0,)');
  });

  it('should decode brackets that the default serializer percent-encoded', () => {
    expect(roundTrip('/graph?range=%5B9.0%5D,%5B9.4%5D')).toBe('/graph?range=[9.0],[9.4]');
  });

  it('should keep the other graph query parameters intact', () => {
    expect(roundTrip('/graph?extended=2&nightly=&range=[9.0,)')).toBe('/graph?extended=2&nightly=&range=[9.0,)');
  });

  it('should not touch a url without a query string', () => {
    expect(roundTrip('/graph/v9.0.0')).toBe('/graph/v9.0.0');
  });

  it('should leave encoded brackets in a path segment alone', () => {
    expect(roundTrip('/graph/%5Bnot-a-range%5D')).toBe('/graph/%5Bnot-a-range%5D');
  });

  it('should preserve a value that literally contains a percent-encoded bracket', () => {
    const tree = serializer.parse('/graph?range=%255B9.0%255D');

    expect(tree.queryParams['range']).toBe('%5B9.0%5D');
    expect(serializer.serialize(tree)).toBe('/graph?range=%255B9.0%255D');
  });

  it('should still encode characters that are unsafe in a query string', () => {
    const tree = serializer.parse('/graph');
    tree.queryParams = { range: '[9.0] #1&2' };

    expect(serializer.serialize(tree)).toBe('/graph?range=[9.0]%20%231%262');
  });
});
