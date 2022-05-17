/**
 * Mocks the React navigation when using useNavigation for tests.
 */
export const mockNavigate = jest.fn();
export const mockAddListener = jest.fn();

jest.mock('@react-navigation/native', () => {
  const actualNavigation = jest.requireActual('@react-navigation/native');
  return {
    ...actualNavigation,
    useNavigation: () => ({
      addListener: () => mockAddListener,
      navigate: mockNavigate,
    }),
  };
});
