const isDockerDevPort =
  typeof window !== 'undefined' && window.location.port === '4201';

export const environment = {
  production: false,
  apiBaseUrl: isDockerDevPort ? 'http://localhost:8081/api/v1' : 'http://localhost:8080/api/v1',
};
