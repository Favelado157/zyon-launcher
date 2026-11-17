'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { publisherConfiguration } = require('../build/configure-publisher.cjs');
test('updates can be published while Microsoft registration is pending', () => {
  const config = publisherConfiguration('https://github.com/Favelado157/zyon-launcher');
  assert.equal(config.githubRepository, 'Favelado157/zyon-launcher');
  assert.equal(config.microsoftClientId, '');
});
test('publisher setup rejects credentials or malformed identifiers in the public Client ID field', () => {
  assert.throws(() => publisherConfiguration('Favelado157/zyon-launcher', 'not-a-client-id'), /Client ID/);
  const id = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee';
  assert.equal(publisherConfiguration('Favelado157/zyon-launcher', id).microsoftClientId, id);
  assert.throws(() => publisherConfiguration('', id), /repositório/);
});
