import type { App } from 'vue'
import hasPermi from './permission/hasPermi'
import dataScope from './permission/dataScope'

export default function setupDirectives(app: App) {
  app.directive('hasPermi', hasPermi)
  app.directive('dataScope', dataScope)
}
