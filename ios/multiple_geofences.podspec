Pod::Spec.new do |s|
  s.name             = 'multiple_geofences'
  s.version          = '0.0.1'
  s.summary          = 'A Flutter plugin for handling geofencing.'

  s.description      = <<-DESC
                       A Flutter plugin for handling multiple geofences using iOS native geofencing capabilities.
                       DESC

  s.homepage         = 'https://github.com/steveroseik'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Name' => 'steveroseik@gmail.com' }
  s.source           = { :path => '.' }
  s.source_files     = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform         = :ios, '14.0'

  # Ensure that the Swift version is set
  s.swift_version = '5.0'
end
