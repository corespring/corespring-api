files = [
  ANGULAR_SCENARIO,
  ANGULAR_SCENARIO_ADAPTER,
  'scenarios.js'
];

urlRoot = '/__testacular/';

autoWatch = false;

singleRun = true;

browsers = ['PhantomJS'];

proxies = {
  '/': 'http://localhost:9000/'
};
