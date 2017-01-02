// groovy code to run post build

if(manager.logContains('.*Release already built.*')) {
  manager.addWarningBadge('Release already built')
  manager.createSummary('warning.gif').appendText(
    '<h1>Build skipped, release has already been built</h1>',
    false, false, false, 'red')
  manager.build.result = hudson.model.Result.NOT_BUILT
}
