import { defineConfig } from 'cypress'

export default defineConfig({
  video: true,
  reporter: 'cypress-multi-reporters',
  reporterOptions: { configFile: 'reporter-config.json' },
  videosFolder: 'test-results/videos',
  screenshotsFolder: 'test-results/screenshot',
  defaultCommandTimeout: 10000,
  pageLoadTimeout: 60000,
  e2e: {
    baseUrl: 'http://localhost:4200',
    specPattern: 'e2e/**/*.cy.{js,jsx,ts,tsx}',
    supportFile: 'support/e2e.ts',
  },
})
