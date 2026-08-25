import Testing
import EnvFlags

@Suite("EnvFlags Swift Export Smoke Tests")
struct EnvFlagsExportTests {
    @Test("EnvFlags swift module imports and exports cleanly")
    func swiftModuleLoads() throws {
        #expect(Bool(true))
    }
}
