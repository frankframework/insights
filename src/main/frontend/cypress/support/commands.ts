/// <reference types="cypress" />
// ***********************************************
// This example commands.ts shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************

// Required for TypeScript to treat this as a module and allow `declare global`
export {};

declare global {
  namespace Cypress {
    interface Chainable {
      dismissReleaseCatalogusModal(): Chainable<void>;
    }
  }
}

/**
 * Dismiss the release catalogus modal if it appears.
 * The modal opens automatically on fresh sessions.
 */
Cypress.Commands.add('dismissReleaseCatalogusModal', () => {
  cy.get('body').then(($body) => {
    if ($body.find('app-modal').length > 0) {
      cy.get('button[aria-label="Close modal"]').click();
    }
  });
});
